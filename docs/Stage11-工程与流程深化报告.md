# Stage 11 工程与流程深化报告

执行日期：2026-09-20

项目目录：`D:\wkk\Student Management System`

本阶段对应《复查问题-分阶段优化提示词》阶段 11，覆盖复查报告第 12-17 项。

## 1. 改动清单

### 1.1 JaCoCo 覆盖率与门槛（第 12 项）

| 文件 | 说明 |
| --- | --- |
| `backend/pom.xml` | 新增 `jacoco-maven-plugin` 0.8.12：`prepare-agent` + `report`（test 阶段）+ `check`（verify 阶段） |

门槛设置与依据：

| 范围 | 门槛 | 当前实测 |
| --- | ---: | ---: |
| BUNDLE 行覆盖 | 25% | 47.8%（有 MySQL 时） / 29.3%（无 MySQL，集成测试跳过） |
| `util`、`interceptor`、`service.ratelimit` | 85% | 94.0% / 87.9% / 89.3% |

说明：集成测试在 MySQL 不可用时自动跳过，因此只对与数据库无关的包设硬门槛，
整体行覆盖设置保守下限，避免无数据库的机器误报失败；`mvn package` 不触发检查，`mvn verify` 才触发。

### 1.2 MockMvc 接口集成测试（第 15 项）

`backend/src/test/java/com/example/score/ApiIntegrationTest.java` 新增 8 个用例：

| 用例 | 断言 |
| --- | --- |
| 三种角色登录 | admin/teacher/student 返回对应角色 |
| 密码错误 | 400 + 「用户名或密码错误」 |
| 未带 token | 401 |
| 学生访问管理员接口 | 403 |
| 成绩提交锁定后修改 | 400 + 锁定提示；管理员解锁后再修改成功 |
| 列表分页 | `page=1&size=3` 返回 total=8/records=3；不传分页返回长度 8 的数组 |
| 学生绩点 | `gpa=3.64`、`totalCredit=14.0` |
| 非法成绩 | 400 + 「成绩必须在 0 到 100 之间」 |

### 1.3 前端 E2E（第 14 项）

| 文件 | 说明 |
| --- | --- |
| `frontend/playwright.config.js` | Playwright 配置（串行、headless、失败截图、`E2E_BASE_URL` 可覆盖） |
| `frontend/e2e/helpers.js` | 登录/退出公共操作；登录后断言已离开登录页，避免后续断言被误导 |
| `frontend/e2e/admin-dashboard.spec.js` | 管理员登录 → 首页概览统计正确；侧边栏进入学生管理并有分页 |
| `frontend/e2e/student-scores.spec.js` | 学生成绩列表可读且有分页；统计页显示平均绩点与学分 |
| `frontend/e2e/teacher-grade-lock.spec.js` | 教师提交成绩 → 界面锁定；管理员解锁 → 恢复录入中（前置用 API 复位状态） |
| `frontend/package.json` | 新增 `npm run e2e` |

### 1.4 CI 与本地校验（第 13 项）

| 文件 | 说明 |
| --- | --- |
| `.github/workflows/ci.yml` | 后端由 `mvn package` 改为 `mvn verify`，纳入覆盖率门槛 |
| `verify.bat` | 新增：一键执行后端 `mvn verify` 与前端 lint + 单元测试 + 构建，输出统一结论 |
| `start.env.example` | 新增可选的 `MVN_CMD`（本机 Maven 不在 PATH 时使用） |

### 1.5 Flyway 数据库迁移（第 16 项）

| 文件 | 说明 |
| --- | --- |
| `backend/pom.xml` | 新增 `flyway-core` 与 `flyway-mysql` |
| `resources/db/migration/V1__init_schema.sql` | 9 张表的建表语句（`CREATE TABLE IF NOT EXISTS`，幂等） |
| `resources/db/migration/V2__seed_demo_data.sql` | 演示数据（`INSERT IGNORE`，幂等）+ 总评回填 |
| `resources/db/migration/V3__incremental_columns.sql` | 为**旧版本数据库**补齐增量字段与审计表（`information_schema` 判断 + 动态 ALTER，MySQL 8 上幂等） |
| `application.yml` | `spring.flyway`：`baseline-on-migrate=true`、`baseline-version=0`、默认启用 |

迁移脚本由 `sql/student_score.sql` 机械转换生成（DDL 去 DROP、数据改 INSERT IGNORE），避免手工抄写偏差。
两者关系：Flyway 负责结构演进，`sql/student_score.sql` 保留为「重建演示库」的独立脚本。

### 1.6 冗余清理（第 17 项）

| 清理项 | 结论 |
| --- | --- |
| `frontend/src/api/teacher.js` 的 `getScores` | 无任何页面引用，已删除 |
| `AdminScoreController`、`AdminStudentController`、`TeacherController` 中失效的 import | 分页改造后不再使用，已清理 |
| `CourseStudentVO.classId` | **未删除**：教师的班级统计页用它构建班级下拉，属于有效字段 |

## 2. 验证结果

### 2.1 后端

```text
mvn -B test   -> Tests run: 67, Failures: 0, Errors: 0
mvn -B verify -> BUILD SUCCESS（含 jacoco:check）
覆盖率        -> 47.84%（行覆盖，含集成测试）
```

覆盖率随测试补充的变化：29.32%（仅单元测试）→ 47.84%（补 MockMvc 接口测试后）。

### 2.2 前端

```text
npx eslint .                 -> 0 问题
npm run test                 -> Tests 10 passed
npm run build                -> 构建成功
npm run e2e                  -> 5 passed (6.7s)
```

E2E 首轮出现 2 通过 3 失败，原因是安装 Playwright 后 Vite 触发依赖重优化并自动重载页面，
与首个用例的输入发生竞态；改进登录辅助方法（先等待输入框、登录后断言离开登录页）后复跑稳定通过。
另有一个用例依赖课程 1 处于未锁定状态，已补 `beforeEach` 通过接口复位，保证可重复运行。

### 2.3 Flyway 双路径验证

| 场景 | 实测 |
| --- | --- |
| 全新数据库（`student_score_fresh`） | Flyway 自动执行 V1、V2；生成 10 张表（9 业务表 + 历史表），sys_user=11、student=8、course=7、score=40、course_student=42，总评无不一致；应用可正常登录与查询统计 |
| 已有数据库（`student_score`） | 记录 baseline v0，V1/V2 幂等执行（仅 MySQL 告警日志），数据未变：sys_user=11、student=8、score=40；应用正常启动、登录返回 200 |
| 验证后处理 | 临时库 `student_score_fresh` 已删除 |

#### 补充：旧版本数据库的升级验证（2026-09-20 追加）

最初只做了 V1/V2，而 V1 用的是 `CREATE TABLE IF NOT EXISTS`，对「已存在但结构较旧」的库**不会补列**。
用 `git show HEAD:sql/student_score.sql` 还原出优化前的建表脚本、建出模拟旧库（8 张表、0 个新字段）后验证发现这个问题，
因此补充了 V3 增量迁移：

| 场景 | 实测 |
| --- | --- |
| 模拟旧库直接启动新版后端 | Flyway baseline v0 → V1 no-op → V2 no-op → **V3 补齐 5 个字段 + `score_change_log` 表** |
| 升级后结构 | `new_columns=5`、表数量 8 → 10（含历史表与审计表） |
| 升级后数据 | student=8、score=40，业务数据未受影响 |
| 升级后功能 | 旧库登录返回 200，新接口 `/api/admin/dashboard` 返回 200 且数据正确 |

即：**服务器上的旧库可以直接换新 JAR 启动，结构会自动补齐**，无需人工执行 ALTER。

另外，接口集成测试原本假设演示账号处于可用状态，管理员一旦正常停用某位教师（例如 `t001`），
测试就会因为登录 400 而失败。已在 `ApiIntegrationTest` 的 `@BeforeEach` 中、于测试事务内临时
恢复演示账号为「启用且无需强制改密」，事务回滚后不影响真实数据；同时保持了对真实数据库状态的独立性。

### 2.4 一键本地校验

```text
verify.bat -> [2/3] 后端 mvn verify：BUILD SUCCESS
              [3/3] 前端 lint + test + build：通过
              全部校验通过
```

`verify.bat` 同样踩到了批处理中文编码问题（UTF-8 无 BOM 导致解析错乱），
已按 `start.bat` 的方式改为 UTF-8 with BOM + CRLF 后验证通过。

### 2.5 数据恢复

```text
score=40、course_student=42、score_change_log=0
锁定课程=0、update_by 非空记录=0、总评不一致记录=0、测试残留学生=0
```

## 3. 未做的部分与原因

| 项目 | 说明 |
| --- | --- |
| CI 在真实 GitHub 环境运行 | 本地无法验证 GitHub Actions 的实际执行结果，工作流已按 `mvn verify` 更新 |
| service 包的硬门槛 | service 覆盖率与数据库可用性强相关（有 MySQL 44.9%、无 MySQL 30.1%），因此只记录不设限 |
| E2E 覆盖全部页面 | 当前覆盖 4 条主流程（管理员首页/学生管理、学生成绩/绩点、教师提交与管理员解锁），未覆盖 Excel 导入导出与改密流程 |
| Playwright 产物目录 | `frontend/test-results/` 为运行期产物，已加入 `.gitignore`，未随代码提交 |

## 4. 结论

```text
PASS
```

阶段 11 的 6 项全部完成：覆盖率统计与门槛、MockMvc 接口测试、Playwright E2E、
CI 与本地一键校验、Flyway 迁移、冗余清理；后端用例从 59 增至 67，前端新增 5 个 E2E 用例。
