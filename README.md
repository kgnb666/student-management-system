# 学生成绩管理系统

一个基于 Vue 3 + Spring Boot + MySQL 的前后端分离课程设计项目，包含管理员、教师、学生三种角色。

## 功能

- 管理员：学生、教师、班级、课程、学期、成绩管理，维护课程学生名单。
- 管理员首页概览：学生/教师/班级/课程规模、当前学期成绩统计与分布图。
- 教师：查看本人课程、维护课程学生成绩（含 Excel 导入导出）、提交成绩锁定、查询成绩、查看课程和班级统计。
- 学生：自助注册（提交后需管理员审核激活）、查看个人信息、本人课程、按学期查询成绩和个人成绩统计。
- 账号安全：三种角色均可自助修改密码；管理员新建账号或重置密码后，本人首次登录必须先改密。
- 统计指标：平均分、最高分、最低分、及格率、优秀率、成绩分布图。
- 学分绩点：学生端按课程学分对 4.0 制绩点加权平均，并显示已获学分/总学分。
- 成绩锁定：教师提交后成绩锁定不可修改，需管理员在课程管理中解锁。
- 成绩审计：新增、修改、删除、提交锁定、解锁都会写入变更日志，可按课程与学生追溯。
- 总评成绩：平时成绩占 30%，期末成绩占 70%。

## 技术栈

- 后端：Spring Boot 3、MyBatis-Plus、JWT、MySQL 8
- 前端：Vue 3、Element Plus、Vue Router、Axios、ECharts
- 构建：Maven、Vite

## 目录结构

```text
Student Management System/
├─ backend/                 Spring Boot 后端
├─ frontend/                Vue 3 前端
├─ sql/student_score.sql    建表及演示数据
└─ README.md
```

## 运行步骤

### 一键启动

直接双击项目根目录下的 `start.bat`。脚本会自动：

1. 检查并启动 MySQL80 服务。
2. 首次运行时导入 `sql/student_score.sql`。
3. 启动后端和前端。
4. 打开 `http://localhost:5173`。

首次运行会自动根据 `start.env.example` 生成配置文件 `start.env`，
数据库密码、端口、Tomcat 协议都在该文件中修改（`start.env` 已加入 `.gitignore`，不会入库）。

如果 `BACKEND_PORT` / `FRONTEND_PORT` 被其他程序占用，脚本会自动改用后面第一个空闲端口并打印提示，
不需要手工改配置；如需固定端口，请修改 `start.env` 并关闭占用端口的程序。

判断「后端是否已在运行」时会同时检查 `/actuator/info` 中的应用标识（`student-score`），
避免把同端口上的其他 Spring Boot 应用误判成本项目后端。

### Docker 一键启动

如果本机已安装 Docker，可以一条命令启动完整环境（MySQL + 后端 + nginx 前端）：

```bash
docker compose up -d --build
```

启动后访问 `http://localhost:8081`（端口可用 `APP_PORT` 覆盖）。
MySQL 首次启动时会自动导入 `sql/student_score.sql`，数据保存在 `mysql-data` 卷中。

停止服务：

```bash
docker compose down          # 保留数据库数据卷
docker compose down -v       # 连数据卷一起删除
```

也可以按下面的步骤手动启动。

### 1. 初始化数据库

使用 MySQL 客户端执行：

```sql
source sql/student_score.sql;
```

脚本会创建 `student_score` 数据库，并写入演示数据。

### 2. 修改数据库配置

编辑 `backend/src/main/resources/application.yml`，将用户名和密码改为本机 MySQL 配置：

```yaml
spring:
  datasource:
    username: root
    password: 123456
```

### 3. 启动后端

在 `backend` 目录执行：

```bash
mvn spring-boot:run
```

如果本地环境执行 `spring-boot:run` 时出现 `ClassNotFoundException`，可以改用：

```bash
mvn -DskipTests package
java -jar target/student-score-1.0.0.jar
```

后端地址：`http://localhost:8080`

### 4. 启动前端

在 `frontend` 目录执行：

```bash
npm install
npm run dev
```

浏览器访问：`http://localhost:5173`

## 演示账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `admin123` |
| 教师 | `t001` 或 `t002` | `123456` |
| 学生 | `2023001` 至 `2023008` | `123456` |

管理员新增学生或教师时，默认登录密码是 `123456`。学生自助注册时由学生设置自己的密码，
注册后账号处于「待审核」状态，需要管理员在学生管理中点击「审核通过」才能登录。

## 主要接口

- 登录：`POST /api/auth/login`
- 注册：`POST /api/auth/register`（注册后为待审核状态）
- 修改密码：`POST /api/auth/change-password`
- 注册班级列表：`GET /api/auth/classes`（只返回班级 id 与名称）
- 管理员：`/api/admin/students`、`/api/admin/teachers`、`/api/admin/classes`、`/api/admin/courses`、`/api/admin/semesters`、`/api/admin/scores`
- 管理员审核注册：`POST /api/admin/students/{id}/approve`
- 管理员首页概览：`GET /api/admin/dashboard`；解锁成绩：`POST /api/admin/courses/{id}/unlock`
- 管理员成绩变更记录：`GET /api/admin/score-logs?courseId=&studentId=&page=&size=`
- 教师：`/api/teacher/courses`、`/api/teacher/scores`、`/api/teacher/courses/{id}/statistics`
- 教师提交成绩：`POST /api/teacher/courses/{id}/submit`；导出成绩单：`GET /api/teacher/courses/{id}/scores/export`；
  导入成绩单：`POST /api/teacher/courses/{id}/scores/import`
- 教师课程变更记录：`GET /api/teacher/courses/{id}/score-logs`
- 学生：`/api/student/profile`、`/api/student/courses`、`/api/student/scores`、`/api/student/statistics`、`/api/student/gpa`

除登录、注册、注册班级列表接口外，请求需要携带：

```text
Authorization: Bearer <token>
```

### 分页参数

管理员列表接口（学生、教师、课程、成绩）支持可选的分页参数：

```text
GET /api/admin/scores?page=1&size=10
```

- 不传 `page` 时保持原有行为，`data` 直接返回数组。
- 传入 `page` 时，`data` 返回 `{ records, total, current, size }`，`size` 最大 100、默认 10。
- 教师与学生接口的数据范围本身受课程/本人限制，未启用分页。

### 开发模式（打印 SQL）

后端默认不打印每条 SQL，需要调试 SQL 时启用 `dev` profile：

```bash
java -jar target/student-score-1.0.0.jar --spring.profiles.active=dev
```

或设置环境变量 `SPRING_PROFILES_ACTIVE=dev`。

## 环境变量

后端配置项都有本地默认值，直接运行无需配置；部署时建议通过环境变量覆盖：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 后端端口 |
| `SPRING_DATASOURCE_URL` | 本机 `student_score` 连接串 | 数据库连接 |
| `SPRING_DATASOURCE_USERNAME` | `root` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | `123456` | 数据库密码 |
| `JWT_SECRET` | 仓库中的演示密钥 | 生产环境必须覆盖 |
| `JWT_EXPIRATION` | `86400000` | token 有效期（毫秒） |
| `LOGIN_MAX_ATTEMPTS` | `5` | 同一账号连续登录失败次数上限 |
| `LOGIN_IP_MAX_ATTEMPTS` | `20` | 同一来源 IP 连续登录失败次数上限 |
| `LOGIN_LOCK_MINUTES` | `15` | 触发上限后的锁定时长（分钟） |
| `REGISTER_MAX_PER_HOUR` | `5` | 同一来源 IP 在窗口内允许的注册次数 |
| `REGISTER_WINDOW_MINUTES` | `60` | 注册限流的窗口长度（分钟） |
| `AUTH_CACHE_MILLIS` | `30000` | 鉴权用户信息缓存时长（毫秒），账号写操作会立即清除 |
| `FLYWAY_ENABLED` | `true` | 是否启用 Flyway 数据库迁移 |
| `TOMCAT_PROTOCOL` | `nio2` | Tomcat 连接器协议，正常环境可改为 `nio` |

前端环境变量（可复制 `frontend/.env.example` 为 `frontend/.env.local`）：

| 变量 | 默认行为 | 说明 |
| --- | --- | --- |
| `VITE_SHOW_DEMO_ACCOUNTS` | 显示（开发环境与生产构建都显示） | 是否在登录页显示演示账号；现为课程设计演示需要，由 `frontend/.env.production` 设为 `true`，正式环境可改为 `false` 后重新构建 |

## 已完成的检查

- 后端测试、覆盖率门槛与打包：`mvn verify`（67 个用例：单元 + MockMvc 接口 + SQL 集成）
- 前端单元测试：`npm run test`（10 个用例）
- 前端端到端测试：`npm run e2e`（5 个用例，需先启动前后端）
- 前端代码规范：`npm run lint`、`npm run format:check`
- 前端生产构建：`npm run build`
- 容器化启动：`docker compose up -d --build`
- 一键本地校验（等价于 CI）：双击 `verify.bat`

## 注意事项

- 运行前先启动 MySQL 并导入 `sql/student_score.sql`。
- 数据库账号密码在 `start.env`（本地启动）或环境变量中配置，`application.yml` 只保留开发默认值。
- 新增学生和教师的默认密码都是 `123456`。
- 管理员新建学生/教师或重置密码后，该账号首次登录会被要求先修改初始密码，改密前无法使用其他功能。
- 注册接口按来源 IP 限流（默认每 60 分钟 5 次），登录失败次数限制为内存实现，单实例生效。
- 总评成绩按平时成绩 30%、期末成绩 70% 计算，保留 1 位小数。
- 学生端学分绩点按 4.0 制、课程学分加权计算，总评不低于 60 的课程计入已获学分。
- 成绩单导入只支持 `.xlsx`，模板列顺序为：学号、姓名、班级、平时成绩、期末成绩、总评成绩。
- 成绩变更日志只追加不修改；升级前已存在的成绩没有历史记录属于正常现象。
- 数据库结构由 Flyway 管理：`V1__init_schema.sql` 建表、`V2__seed_demo_data.sql` 写入演示数据、
  `V3__incremental_columns.sql` 为旧版本数据库补齐增量字段与审计表，三者都幂等；
  **旧版本数据库直接换新 JAR 启动即可自动补列**。`sql/student_score.sql` 是「重建演示库」的
  独立脚本（先 DROP 再重建），Docker 首次启动与手工重置数据用它。
- 后端覆盖率门槛在 `mvn verify` 阶段检查（`mvn package` 不触发）；集成测试与接口测试在
  MySQL 不可用时会自动跳过。
- 前端 E2E 需要先启动后端与前端（默认地址 `http://localhost:15173`，可用 `E2E_BASE_URL` 覆盖），
  再执行 `npm run e2e`。
- 登录页的演示账号支持**点击自动填充**（点一下账号行，用户名密码直接写入表单）；
  如需在正式环境隐藏，把 `frontend/.env.production` 中的 `VITE_SHOW_DEMO_ACCOUNTS` 改为 `false` 后重新构建部署。
- 账号被停用、处于待审核状态或密码被重置后，此前签发的 token 会立即失效，需要重新登录。
- 部分 Windows + JDK 组合下 Tomcat 默认 NIO 会报 `Unable to establish loopback connection`，
  因此 `TOMCAT_PROTOCOL` 默认是等价的 `nio2`；正常的 Linux 环境可以设置为 `nio` 使用标准 NIO。
- 已在旧版本数据库上运行过的环境，请重新导入 `sql/student_score.sql`（会重建演示数据），
  或手动补齐新增字段：`sys_user.token_version`、`score.update_by`、
  `sys_user.need_change_password`、`course.score_status`、`course.submit_time`。
  成绩审计表 `score_change_log` 需要按 `sql/student_score.sql` 中的定义手动创建。
- 部署脚本 `deploy/deploy-student-management.sh` 的环境相关取值（站点域名、端口、容器名、nginx 目录等）
  全部可通过环境变量覆盖，脚本头部有完整的可配置项列表与前置条件说明。
- 如果部署环境中本项目的数据库与其他项目共用容器，可执行 `deploy/decouple-database.sh` 拆分：
  脚本会先备份数据，再新建独立的 `student-score-mysql` 容器与 `student-score_default` 网络、
  导入数据并重建后端容器，最后停掉旧数据库容器（容器与数据卷保留以便回滚）。
- 站点默认同时监听 `80` 与 `889`，两者都需要在云服务器安全组 / 防火墙放行后才能从外网访问；
  后端容器的资源限制可用 `BACKEND_MEMORY`、`BACKEND_CPUS`、`BACKEND_XMX` 覆盖（默认 448m / 1.0 核 / 256m）。
