# Stage 4 工程化与可维护性报告

执行日期：2026-09-20

项目目录：`D:\wkk\Student Management System`

本阶段按《分阶段优化提示词》Stage 4 执行：补测试、加规范、加文档与健康检查、加 CI。

## 1. 改动清单

### 1.1 后端测试（14 个用例）

| 测试类 | 覆盖内容 | 用例数 |
| --- | --- | --- |
| `service/ScoreServiceTest` | 总评计算（80/90、60/70、90/86）、边界（0/100、100/0、100/100）、只填一项不计算、统计组装、空数据返回全 0 | 5 |
| `interceptor/JwtInterceptorTest` | 无 token 401、OPTIONS 放行、停用账号 401、token 版本失效 401、学生访问管理员接口 403、角色以数据库为准、合法请求放行并写入属性 | 7 |
| `mapper/ScoreMapperAggregateTest` | SQL 聚合与明细计算一致、五档边界（59/60/69/70/79/80/89/90）归类正确 | 2 |

设计说明：

- 单元测试全部使用 Mockito，不依赖 MySQL。
- SQL 聚合集成测试使用真实 MySQL 并在事务中回滚；MySQL 不可用时自动跳过，不影响单元测试。
- 集成测试中发现并处理了一个真实陷阱：`JdbcTemplate` 的更新不经过 MyBatis，
  同一事务内会命中 MyBatis 会话级缓存，因此测试中显式调用 `SqlSessionTemplate.clearCache()`。

### 1.2 前端测试（9 个用例）

引入 `vitest` + `@vue/test-utils` + `jsdom`，新增 `npm run test`：

| 测试文件 | 覆盖内容 | 用例数 |
| --- | --- | --- |
| `src/auth.spec.js` | 保存/读取登录态、未登录返回空、脏数据自愈、清除登录态、按角色返回首页 | 5 |
| `src/views/Register.spec.js` | 缺少必填项、密码长度不足、两次密码不一致均拦截且不提交，校验通过时提交并提示待审核 | 4 |

### 1.3 代码规范

| 文件 | 说明 |
| --- | --- |
| `eslint.config.js` | ESLint 9 扁平配置：`@eslint/js` + `eslint-plugin-vue` + `eslint-config-prettier` |
| `.prettierrc` / `.prettierignore` | Prettier 配置（无分号、单引号、行宽 100） |
| `package.json` | 新增 `lint`、`format`、`format:check` 脚本 |

ESLint 首次运行即 0 问题。Prettier 对 15 个文件做了纯格式化调整（不涉及逻辑），
格式化后重新执行了 lint、单元测试与生产构建，全部通过。

### 1.4 接口文档与健康检查

| 文件 | 改动 |
| --- | --- |
| `pom.xml` | 新增 `spring-boot-starter-actuator`、`springdoc-openapi-starter-webmvc-ui` |
| `application.yml` | 仅暴露 `health`、`info` 端点；Swagger 路径 `/swagger-ui.html` |

### 1.5 统一异常处理补强

验证过程中发现：未被路由匹配的请求会被全局兜底处理成 500。

`exception/GlobalExceptionHandler.java` 新增：

| 异常 | 状态码 | 提示 |
| --- | --- | --- |
| `NoResourceFoundException` | 404 | 接口不存在 |
| `HttpRequestMethodNotSupportedException` | 405 | 请求方法不被支持 |
| `HttpMediaTypeNotSupportedException` | 415 | 请求内容类型不被支持 |

### 1.6 CI

`.github/workflows/ci.yml`：后端 Job 使用 MySQL 8 服务容器，导入 `sql/student_score.sql` 后执行 `mvn -B package`；
前端 Job 执行 `npm ci` → `npm run lint` → `npm run test` → `npm run build`。

## 2. 验证结果

### 2.1 后端

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS (Total time: 8.4s / 13.9s 含打包)
```

测试有效性抽查（突变测试）：把总评公式中平时成绩权重从 `0.3` 临时改为 `0.4` 后执行
`mvn test -Dtest=ScoreServiceTest`，结果为 `Tests run: 5, Failures: 2`、`BUILD FAILURE`；
恢复原公式后重新执行为 `Failures: 0`。说明成绩计算相关的测试确实能拦住改错。

### 2.2 前端

```text
Test Files  2 passed (2)
Tests       9 passed (9)
Duration    2.20s

npx eslint .                  -> 0 问题
npx prettier --check src      -> All matched files use Prettier code style
npm run build                 -> 构建成功（6.1s）
```

### 2.3 接口文档与健康检查

| 请求 | 实测 | 结果 |
| --- | --- | --- |
| `GET /actuator/health` | HTTP 200，`{"status":"UP"}` | PASS |
| `GET /actuator/info` | HTTP 200，`{}` | PASS |
| `GET /actuator/env`（未暴露） | HTTP 404 | PASS |
| `GET /v3/api-docs` | HTTP 200，生成全部控制器路径 | PASS |
| `GET /swagger-ui/index.html` | HTTP 200 | PASS |

### 2.4 异常处理验证

| 请求 | 修复前 | 修复后 | 结果 |
| --- | --- | --- | --- |
| `GET /api/not-exists`（带 token） | 500 | 404「接口不存在」 | PASS |
| `GET /api/auth/login`（方法不支持） | 500 | 405「请求方法不被支持」 | PASS |
| `POST /api/auth/login`（非法 JSON） | 500 | 400「请求内容格式不正确」 | PASS |
| `GET /actuator/env` | 500 | 404 | PASS |

## 3. 未做的部分与原因

| 项目 | 说明 |
| --- | --- |
| CI 配置的实际运行验证 | 本地没有 GitHub 运行环境，工作流文件已完成语法与步骤设计，未在真实 CI 上跑过 |
| 覆盖率统计 | 未接入 JaCoCo/覆盖率门槛，当前规模下用例已覆盖核心计算、鉴权与统计口径 |
| 接口注解细化 | 仅保证 OpenAPI 能自动生成，未逐字段补充 `@Operation` 描述，避免大范围改动 Controller |
| 全量代码格式化之外的风格调整 | 只应用 Prettier 格式化，未按 ESLint 建议重构既有写法（当前 lint 已是 0 问题） |

## 4. 结论

```text
PASS
```

项目从「没有任何自动化测试」变为后端 14 个 + 前端 9 个用例，且经突变测试验证有效；
代码规范、接口文档、健康检查与 CI 全部就位，并顺带修复了 404/405 被吞成 500 的真实缺陷。
