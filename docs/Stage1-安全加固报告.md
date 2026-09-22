# Stage 1 安全加固报告

执行日期：2026-09-20

项目目录：`D:\wkk\Student Management System`

本阶段按《分阶段优化提示词》Stage 1 执行，只做安全加固，未改动成绩计算公式、统计口径与既有接口语义。

## 1. 改动清单

### 1.1 JWT 失效能力

| 文件 | 改动 |
| --- | --- |
| `sql/student_score.sql` | `sys_user` 新增 `token_version INT NOT NULL DEFAULT 0` |
| `entity/User.java` | 新增 `tokenVersion` 字段 |
| `util/JwtUtil.java` | token 中写入 `tokenVersion` 声明 |
| `interceptor/JwtInterceptor.java` | 每次请求回查用户：校验 `status=1`、校验 `tokenVersion` 一致，并以数据库角色为准鉴权 |
| `service/StudentService.java` | 重置密码、变更账号状态时自增 `token_version`；新增 `approve` |
| `service/TeacherService.java` | 重置密码、变更账号状态时自增 `token_version` |

原风险：token 一旦签发，在 24 小时内无法撤销；停用账号或重置密码后，旧 token 仍可继续访问。

### 1.2 收紧开放注册

| 文件 | 改动 |
| --- | --- |
| `service/AuthService.java` | 注册账号状态改为「待审核」（`status=2`）；补充学号/姓名/电话长度校验；班级查询返回最小字段 |
| `vo/ClassOptionVO.java` | 新增，只包含班级 `id` 与 `className` |
| `controller/AuthController.java` | `/api/auth/classes` 返回 `ClassOptionVO` |
| `controller/AdminStudentController.java` | 新增 `POST /api/admin/students/{id}/approve` |
| `service/StudentService.java` | 新增 `approve`：激活学生与其登录账号 |
| `frontend/src/views/admin/StudentList.vue` | 状态显示「正常 / 待审核 / 停用」，新增「审核通过」操作 |
| `frontend/src/api/admin.js` | 新增 `approveStudent` |
| `frontend/src/views/Register.vue` | 注册成功提示改为「等待管理员审核」 |

原风险：任何人可用任意学号注册并立即登录；`/api/auth/classes` 匿名可读且返回班主任等完整字段。

### 1.3 登录防爆破

| 文件 | 改动 |
| --- | --- |
| `service/LoginAttemptService.java` | 新增，按账号与来源 IP 统计连续失败次数，超限后锁定 |
| `util/ClientIpUtil.java` | 新增，解析 `X-Forwarded-For` / `X-Real-IP` / `RemoteAddr` |
| `service/AuthService.java` | 登录前校验锁定状态，失败计数、成功清零 |
| `controller/AuthController.java` | 登录接口传入来源 IP |
| `application.yml` | 新增 `security.login.*` 配置项 |

说明：计数保存在内存中，服务重启后清零，多实例部署时各实例独立计数；跨实例共享需改用 Redis。
锁定返回 HTTP 429，前端请求封装对非 401 状态统一走错误提示分支，不需要改动。

### 1.4 CORS 收紧

`config/WebConfig.java`：关闭 `allowCredentials`（前端使用 `Authorization` 头而非 Cookie），保留开发地址与生产域名白名单，新增 `maxAge(3600)`。

### 1.5 配置外置

`application.yml`：端口、数据库连接、JWT、登录限制全部改为 `${ENV:默认值}`，本地零配置仍可启动；
服务器部署脚本已注入的 `SPRING_DATASOURCE_*`、`JWT_SECRET` 环境变量继续生效。

### 1.6 演示账号开关

`frontend/src/views/Login.vue`：演示账号区块改为条件渲染，默认「开发环境显示、生产构建隐藏」，
可用 `VITE_SHOW_DEMO_ACCOUNTS` 显式控制；新增 `frontend/.env.example`。

## 2. 验证结果

### 2.1 构建

| 命令 | 结果 |
| --- | --- |
| `mvn -B -DskipTests package` | BUILD SUCCESS（5.2 s） |
| `npm run build` | 构建成功（7.3 s） |

### 2.2 真实接口验证

后端以 `SERVER_PORT=18811` 启动，连接真实 MySQL 8.0.46 验证：

| 场景 | 预期 | 实测 | 结果 |
| --- | --- | --- | --- |
| 匿名访问 `/api/auth/classes` | 只返回 id 与 className | `{"id":2,"className":"计算机科学2302班"},{"id":1,"className":"软件工程2301班"}` | PASS |
| 学生自助注册 | 注册成功但不可登录 | HTTP 200，登录返回「账号待管理员审核激活」 | PASS |
| 管理员列表中的注册学生 | 状态为待审核 | `status=2` | PASS |
| 管理员审核通过 | 可以登录 | HTTP 200，登录成功并签发 token | PASS |
| 管理员重置该生密码 | 旧 token 失效 | 旧 token 访问 `/api/student/profile` 返回 401「登录状态已失效」 | PASS |
| 重置后用新密码登录 | 成功 | HTTP 200 | PASS |
| 管理员停用该生 | 旧 token 失效 | 返回 401「账号不可用」 | PASS |
| 停用状态下登录 | 被拒绝 | HTTP 400「账号已停用」 | PASS |
| 恢复为正常后登录 | 成功 | HTTP 200 | PASS |
| 连续 5 次登录失败 | 第 6 次被锁定 | 第 6 次返回 HTTP 429「登录失败次数过多，请 15 分钟后重试」 | PASS |

### 2.3 数据清理

| 项目 | 结果 |
| --- | --- |
| 临时测试学生 | 0（已删除） |
| `sys_user` | 11 |
| `student` | 8 |
| `course` | 7 |
| `score` | 40 |

数据库已恢复到 Stage 0 基线。

## 3. 未做的部分与原因

| 项目 | 说明 |
| --- | --- |
| 前端页面交互的浏览器验证 | 本阶段前端改动（状态标签、审核按钮、演示账号开关）通过生产构建与代码审查验证，浏览器交互验证留待 Stage 4 引入前端测试后覆盖 |
| 登录锁定跨实例共享 | 当前为单实例课程项目，内存实现已足够；跨实例需引入 Redis，不在本阶段范围 |
| 数据库迁移脚本 | 项目使用整库重建脚本 `sql/student_score.sql`，本次已同步更新；README 中补充了旧库的 `ALTER TABLE` 说明，未引入 Flyway 等迁移框架 |

## 4. 结论

```text
PASS
```

Stage 1 列出的 6 项安全问题全部修复并完成真实接口验证，构建通过，演示数据未被破坏。
