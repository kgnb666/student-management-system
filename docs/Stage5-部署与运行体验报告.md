# Stage 5 部署与运行体验报告

执行日期：2026-09-20

项目目录：`D:\wkk\Student Management System`

本阶段按《分阶段优化提示词》Stage 5 执行：配置外置、Docker 化、启动与部署脚本改造、协议开关。

## 1. 改动清单

### 1.1 一键启动脚本改造

| 文件 | 说明 |
| --- | --- |
| `start.env.example` | 新增：数据库连接、端口、Tomcat 协议、可选 JAVA_HOME 的配置模板 |
| `start.bat` | 重写：移除写死的数据库密码与固定的 Java 路径判定 |

具体变化：

- 首次运行自动把 `start.env.example` 复制为 `start.env`，并在提示中说明需要按需修改；
  未配置 `MYSQL_PASSWORD` 时直接给出明确报错，不再静默使用写死的密码。
- 数据库主机/端口/用户/密码全部来自 `start.env`，同时作为 `SPRING_DATASOURCE_*` 环境变量传给后端，
  保证初始化脚本与后端使用同一套连接信息。
- Java 查找顺序：`JAVA_HOME` → `PATH` → `%ProgramFiles%\Java\jdk-17*` →
  `%ProgramFiles%\Eclipse Adoptium\jdk-17*` → `D:\tools\jdk\jdk-17*` → DBeaver 自带 JRE，
  并在启动前打印实际使用的 Java 路径。
- 端口占用判定由「端口是否被监听」升级为「健康检查是否返回 200」：
  后端探测 `/actuator/health`，前端探测首页是否包含系统标题；
  端口被其他程序占用时不再误判为「服务已在运行」。

> 后续修正（2026-09-20）：最初的做法是「端口被占用就直接中止脚本」，实测发现当本机 8080
> 被其他项目长期占用时，一键启动会整段失败、前端根本起不来。现已改为自动向后寻找空闲端口
> （最多尝试 10 个）并打印提示，前端代理目标同步跟随，无需手工改 `start.env`。

> 第二次修正（2026-09-20）：健康检查只看 `/actuator/health` 仍不够——同端口上的**其他
> Spring Boot 应用**也会返回 200 UP（实测本机 8080 上是另一个项目 `com.library.Application`），
> 于是脚本误判「本项目后端已在运行」，只启动前端，前端把 `/api` 代理到了别人的后端，表现为
> 「页面能打开、登录一直失败」。现改为检查 `/actuator/info` 中的应用标识 `student-score`
> （`application.yml` 增加 `info.app.name` 与 `management.info.env.enabled`），
> 识别失败即按端口占用处理并自动换端口。
- 前端代理目标随 `BACKEND_PORT` 变化：`start.bat` 设置 `VITE_PROXY_TARGET`，
  `vite.config.js` 读取该变量作为 `/api` 代理目标。
- `start.env` 已加入 `.gitignore`，避免数据库密码入库。

### 1.2 Docker 化

| 文件 | 说明 |
| --- | --- |
| `backend/Dockerfile` | 多阶段构建：Maven + JDK 17 打包 → JRE 运行 |
| `backend/.dockerignore` | 排除 target、IDE 文件 |
| `frontend/Dockerfile` | 多阶段构建：Node 20 构建 → nginx 托管 |
| `frontend/nginx.conf` | 静态资源 + `/api` 反向代理到 `backend:8080` |
| `frontend/.dockerignore` | 排除 node_modules、dist、本地环境文件 |
| `docker-compose.yml` | MySQL 8.4 + 后端 + 前端三个服务，含健康检查与数据卷 |

要点：

- MySQL 首次启动自动执行 `sql/student_score.sql` 建库建表并写入演示数据；
- 后端通过 `depends_on: condition: service_healthy` 等待数据库就绪后再启动；
- 端口、密码、JWT 密钥都可用环境变量覆盖（`APP_PORT`、`MYSQL_PASSWORD`、`JWT_SECRET`）。

### 1.3 协议开关

`TomcatNio2Config` 由「无条件生效」改为 `@ConditionalOnProperty(name = "tomcat.protocol", havingValue = "nio2")`，
`application.yml` 增加 `tomcat.protocol: ${TOMCAT_PROTOCOL:nio2}`。

保留 `nio2` 作为默认值的依据（本机实测）：

```text
设置 TOMCAT_PROTOCOL=nio 启动 -> Caused by: java.io.IOException: Unable to establish loopback connection
保持默认 nio2           启动 -> Tomcat started on port 18811 (http)
```

也就是说，这台 Windows 机器上的默认 NIO 确实无法启动，因此保留 `nio2` 才能开箱可用；
正常的 Linux/容器环境可通过 `TOMCAT_PROTOCOL=nio` 切回标准 NIO。

### 1.4 部署脚本参数化

`deploy/deploy-student-management.sh` 重写：脚本头部集中声明全部可配置项，均支持环境变量覆盖，
并补充了前置条件说明（Docker、MySQL 容器、JAR 与前端构建产物）。

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `APP_DIR` | `/www/wwwroot/student_management` | 应用目录 |
| `SERVER_NAME` | 本机第一个 IP | nginx server_name |
| `SITE_PORT` / `EXTRA_SITE_PORT` | `80` / `889` | 站点监听端口 |
| `NGINX_CONF_DIR` / `NGINX_CONF_NAME` | 宝塔面板默认路径 | nginx 配置位置 |
| `DB_CONTAINER` / `DB_NETWORK` | `campus-ledger-mysql` / `campus-ledger_default` | 数据库容器与网络 |
| `BACKEND_CONTAINER` / `BACKEND_IMAGE` | `student-management-backend` / `campus-ledger-backend:latest` | 后端容器 |
| `BACKEND_HOST_PORT` | `18081` | 后端容器映射端口 |

数据库密码与 JWT 密钥仍由 `openssl rand` 现场生成，写入权限为 600 的 `backend.env`。

## 2. 验证结果

### 2.1 Docker 全链路验证

```text
docker compose config --quiet        -> OK
docker compose up -d --build         -> 三个镜像构建成功，容器全部启动
```

| 验证项 | 实测 | 结果 |
| --- | --- | --- |
| 容器状态 | `student-score-mysql (healthy)`、`student-score-backend (Up)`、`student-score-frontend (Up)` | PASS |
| 前端首页 | `GET http://127.0.0.1:8081/` → 200，包含 `id="app"` | PASS |
| 经 nginx 登录 | `POST /api/auth/login`（admin/admin123）→ 200，返回 ADMIN 角色 | PASS |
| 演示数据导入 | `GET /api/admin/students?page=1&size=5` → `total=8` | PASS |
| 分页接口 | `GET /api/admin/scores?page=1&size=3` → 200 | PASS |
| 统计接口 | 学生 2023001 第二学期平均分 86.6、及格率 100（与历史验收记录一致） | PASS |
| 容器内健康检查 | `wget http://127.0.0.1:8080/actuator/health` → `{"status":"UP"}` | PASS |

验证完成后执行 `docker compose down` 清理容器，仅保留 `mysql-data` 数据卷。

### 2.2 一键启动脚本验证

| 场景 | 实测 | 结果 |
| --- | --- | --- |
| 首次运行 | 自动生成 `start.env` 并提示按需修改 | PASS |
| 后端端口 8080 被其他程序占用 | 输出「端口 8080 已被其他程序占用，无法启动后端」并停止 | PASS |
| 修改为 18811 / 15173 后运行 | 后端与前端均启动成功 | PASS |
| 前端经 Vite 代理访问后端 | `POST http://127.0.0.1:15173/api/auth/login` → 200，角色 ADMIN | PASS |
| 健康检查 | `GET http://127.0.0.1:18811/actuator/health` → 200 | PASS |

过程中发现并修复了一个真实问题：重写后的 `start.bat` 最初出现语句被截断、
`'NTEND_RUNNING' 不是内部或外部命令` 之类的解析错误。原因是批处理文件为 UTF-8 无 BOM，
cmd.exe 解析含中文的行时发生字节错位。改为 **UTF-8 with BOM + CRLF** 后解析正常、中文输出正常。

### 2.3 部署脚本验证

```text
docker run --rm -v ".../deploy:/d:ro" eclipse-temurin:17-jre bash -n /d/deploy-student-management.sh
-> deploy script syntax OK
docker compose config --quiet -> OK
```

本机没有可用的 Bash（`bash.exe` 是未安装发行版的 WSL 存根），因此脚本语法检查在容器内完成。

## 3. 未做的部分与原因

| 项目 | 说明 |
| --- | --- |
| 部署脚本在真实服务器上执行 | 脚本涉及远端服务器 Docker、宝塔面板 nginx 与域名，属于外部环境，本次只完成参数化与语法校验 |
| Docker 镜像推送到镜像仓库 | 当前 compose 采用本地构建，未引入镜像仓库流程 |
| `start.bat` 的自动化测试 | 批处理脚本难以单元测试，采用真实执行 + 端口冲突/成功路径两种场景人工验证 |

## 4. 结论

```text
PASS
```

配置外置、Docker 化、启动脚本与部署脚本改造全部完成；Docker 环境经真实构建与端到端访问验证，
一键启动脚本经两种场景实测，并修复了批处理中文编码导致的解析错误。
