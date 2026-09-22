# Stage 8 账号安全与注册体验报告

执行日期：2026-09-20

项目目录：`D:\wkk\Student Management System`

本阶段对应《复查问题-分阶段优化提示词》阶段 8，覆盖复查报告第 1、2、3、4、6 项。

## 1. 改动清单

### 1.1 自助修改密码

| 文件 | 说明 |
| --- | --- |
| `dto/ChangePasswordRequest.java` | 新增：原密码、新密码、确认新密码（含长度校验） |
| `service/AuthService.java` | 新增 `changePassword`：校验原密码、新密码不与原密码相同、两次输入一致 |
| `controller/AuthController.java` | 新增 `POST /api/auth/change-password`（需登录） |
| `frontend/src/views/ChangePassword.vue` | 新增加密修改页（强制改密与主动改密共用） |
| `frontend/src/api/auth.js` | 新增 `changePassword` |
| `frontend/src/layout/MainLayout.vue` | 顶部用户区新增「修改密码」入口 |

改密成功后会把 `sys_user.token_version` 自增，旧 token 立即失效，前端清除本地登录态并回到登录页。

### 1.2 首次登录强制改密

| 文件 | 说明 |
| --- | --- |
| `sql/student_score.sql` | `sys_user` 新增 `need_change_password TINYINT NOT NULL DEFAULT 0` |
| `entity/User.java`、`dto/LoginResponse.java` | 新增该标记，登录响应返回 `needChangePassword` |
| `service/StudentService.java`、`service/TeacherService.java` | 管理员新建账号与重置密码时置 1；学生自助注册时置 0 |
| `interceptor/JwtInterceptor.java` | 标记为 1 时，除 `/api/auth/**` 外的接口一律返回 403「请先修改初始密码」 |
| `frontend/src/router/index.js` | 路由守卫：未改密的登录用户被强制留在改密页 |
| `frontend/src/views/Login.vue` | 登录后若需改密，直接跳转到改密页并提示 |

### 1.3 注册接口频率限制

| 文件 | 说明 |
| --- | --- |
| `service/RegisterRateLimiter.java` | 新增：按来源 IP 的滑动窗口限流，超限返回 429 |
| `controller/AuthController.java` | 注册前调用限流校验 |
| `application.yml` | 新增 `security.register.max-per-hour`（默认 5）与 `window-minutes`（默认 60） |

**边界说明**：限流为进程内实现，服务重启后清零、多实例之间不共享；如需跨实例生效，
提供一个新的 `AttemptStore` 实现并标注 `@Primary` 即可（见下）。

### 1.4 失败计数存储抽象

| 文件 | 说明 |
| --- | --- |
| `service/ratelimit/AttemptRecord.java` | 新增：失败次数、锁定截止时间与最后失败时间 |
| `service/ratelimit/AttemptStore.java` | 新增：存储接口（get/put/remove/entries） |
| `service/ratelimit/InMemoryAttemptStore.java` | 新增：默认进程内实现 |
| `service/LoginAttemptService.java` | 改为依赖 `AttemptStore`，行为与阈值保持不变 |

本次**没有**引入 Redis 依赖，只是把扩展点留出来。

### 1.5 待审核注册提醒

| 文件 | 说明 |
| --- | --- |
| `vo/DashboardVO.java`、`service/DashboardService.java` | 概览新增 `pendingStudentCount` |
| `frontend/src/views/admin/Dashboard.vue` | 新增「待审核注册」统计卡，有数据时可点击 |
| `mapper/StudentMapper` + `StudentMapper.xml`、`service/StudentService.java`、`controller/AdminStudentController.java` | 学生列表新增 `status` 过滤参数 |
| `frontend/src/views/admin/StudentList.vue` | 新增状态筛选下拉，并支持从首页带 `?status=2` 跳转自动过滤 |

## 2. 验证结果

### 2.1 构建与测试

| 命令 | 结果 |
| --- | --- |
| `mvn -B test` | `Tests run: 55, Failures: 0, Errors: 0`（新增 AuthServiceTest 4、RegisterRateLimiterTest 2、鉴权强制改密 2） |
| `npx eslint .` | 0 问题 |
| `prettier --check` | All matched files use Prettier code style |
| `npm run test` | `Test Files 3 passed`、`Tests 10 passed` |
| `npm run build` | 构建成功（7.0 s） |

### 2.2 真实接口验证

后端以 `SERVER_PORT=18811 REGISTER_MAX_PER_HOUR=1` 启动，连接真实 MySQL：

| 场景 | 实测结果 |
| --- | --- |
| 概览待审核数（注册前） | `pendingStudentCount=0` |
| 自助注册一名学生 | HTTP 200 |
| 概览待审核数（注册后） | `pendingStudentCount=1` |
| `GET /api/admin/students?status=2` | `total=1`，学号 S8X001、status=2 |
| 同 IP 再次注册 | HTTP 429「注册提交过于频繁，请稍后再试」 |
| 管理员新建学生后用默认密码登录 | HTTP 200，`needChangePassword=1` |
| 未改密访问业务接口 | HTTP 403「请先修改初始密码」 |
| 用错误原密码改密 | HTTP 400「原密码不正确」 |
| 正确改密 | HTTP 200 |
| 改密前的旧 token 访问接口 | HTTP 401「登录状态已失效，请重新登录」 |
| 用新密码重新登录 | HTTP 200，`needChangePassword=0` |
| 改密后访问业务接口 | HTTP 200 |

### 2.3 数据恢复

```text
sys_user 11、student 8、teacher 2、class_info 2、semester 2、course 7、course_student 42、score 40
测试残留学生=0、need_change_password=1 的账号=0
```

## 3. 未做的部分与原因

| 项目 | 说明 |
| --- | --- |
| 跨实例限流存储 | 本阶段只抽象出 `AttemptStore` 扩展点，未引入 Redis，符合提示词要求 |
| 浏览器交互验证 | 本阶段前端改动（改密页、路由守卫、待审核卡片）通过构建、单元测试与后端接口验证；真实浏览器验证已在 Stage 6 建立，本阶段未重复执行 |
| 改密的密码强度策略 | 仍只要求不少于 6 位，未引入大小写/符号复杂度要求，避免影响既有演示账号 |

## 4. 结论

```text
PASS
```

阶段 8 的 5 项全部完成：自助改密、首次登录强制改密、注册限流、待审核提醒、失败计数存储抽象；
后端测试从 47 增至 55，真实链路验证覆盖了改密前后 token 失效与 403 拦截。
