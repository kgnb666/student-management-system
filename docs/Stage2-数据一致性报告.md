# Stage 2 数据一致性与业务校验报告

执行日期：2026-09-20

项目目录：`D:\wkk\Student Management System`

本阶段按《分阶段优化提示词》Stage 2 执行，未改动成绩计算公式（平时 30% + 期末 70%，保留 1 位小数）、
统计口径（及格 60、优秀 90、五档分布）与既有接口路径。

## 1. 改动清单

### 1.1 成绩更新不允许换课换人

`service/ScoreService.java`：`update` 改为以记录自身的 `course_id`、`student_id` 为准，
只接受分数变更，不再写入请求中的课程与学生；同时补上「请至少填写一项成绩」校验。

原风险：更新时可覆盖课程与学生，既能把成绩“搬”到另一门课，也可能触发 `uk_course_student_score` 唯一键冲突。

### 1.2 移出名单的连带影响可见

| 文件 | 改动 |
| --- | --- |
| `service/CourseService.java` | `assignStudents` 返回被连带删除的成绩条数 |
| `controller/AdminCourseController.java` | `PUT /api/admin/courses/{id}/students` 的 `data` 返回删除条数 |
| `frontend/src/views/admin/CourseList.vue` | 名单弹窗显示影响提示条，保存前二次确认，保存后提示实际删除条数 |
| `frontend/src/views/admin/ScoreList.vue` | 编辑成绩时课程/学生下拉框禁止修改，并给出说明 |

### 1.3 名单入参去重与存在性校验

`service/CourseService.java`：`studentIds` 先去空去重；再用一次 `IN` 查询校验学生是否存在，
不匹配时提示「学生名单中存在无效的学生，请刷新后重试」。

### 1.4 唯一性预校验

| 文件 | 改动 |
| --- | --- |
| `service/StudentService.java` | 新增/改学号前检查 `sys_user.username` 与 `student.student_no`，提示「该学号已被占用」 |
| `service/TeacherService.java` | 新增/改工号前检查 `sys_user.username` 与 `teacher.teacher_no`，提示「该工号已被占用」 |

### 1.5 Bean Validation

| 文件 | 改动 |
| --- | --- |
| `pom.xml` | 新增 `spring-boot-starter-validation` |
| `dto/LoginRequest.java` | `@NotBlank` |
| `dto/RegisterRequest.java` | `@NotBlank` / `@Size` / `@NotNull` |
| `dto/ScoreRequest.java`、`dto/ScoreItem.java` | `@NotNull`、`@DecimalMin(0)`、`@DecimalMax(100)` |
| `dto/BatchScoreRequest.java` | `@NotNull`、`@Valid` |
| `controller/AuthController.java`、`AdminScoreController.java`、`TeacherController.java` | 增加 `@Valid` |
| `exception/GlobalExceptionHandler.java` | 统一处理 `MethodArgumentNotValidException`、`HttpMessageNotReadableException`、参数缺失/类型错误 |

校验消息与原有 Service 提示保持一致，Service 中的业务规则校验全部保留。

### 1.6 成绩留痕

| 文件 | 改动 |
| --- | --- |
| `sql/student_score.sql` | `score` 新增 `update_by BIGINT NULL` |
| `entity/Score.java`、`vo/ScoreVO.java` | 新增 `updateBy` / `updaterName` |
| `service/ScoreService.java` | 新增、修改、批量保存时记录当前登录用户 id |
| `resources/mapper/ScoreMapper.xml` | 左连 `sys_user` 输出 `updater_name` |
| `frontend/src/views/admin/ScoreList.vue` | 新增「最后修改人」列 |

`score.update_time` 由数据库 `ON UPDATE CURRENT_TIMESTAMP` 维护，与 `update_by` 共同构成修改留痕。

## 2. 验证结果

### 2.1 构建

| 命令 | 结果 |
| --- | --- |
| `mvn -B -DskipTests package` | BUILD SUCCESS（6.6 s） |
| `npm run build` | 构建成功（8.0 s） |

### 2.2 真实接口验证

后端以 `SERVER_PORT=18812` 启动，连接真实 MySQL 8.0.46：

| 场景 | 预期 | 实测 | 结果 |
| --- | --- | --- | --- |
| 平时成绩 101 | 拒绝 | HTTP 400「成绩必须在 0 到 100 之间」 | PASS |
| 平时成绩 -1 | 拒绝 | HTTP 400「成绩必须在 0 到 100 之间」 | PASS |
| 缺少课程 id | 拒绝 | HTTP 400「请选择课程和学生」 | PASS |
| 空用户名密码登录 | 拒绝 | HTTP 400「请输入用户名和密码」 | PASS |
| 学号改成已存在的 2023002 | 拒绝 | HTTP 400「该学号已被占用」 | PASS |
| 工号改成已存在的 t002 | 拒绝 | HTTP 400「该工号已被占用」 | PASS |
| 更新成绩时改课程/学生 | 课程学生不变 | 记录仍为 `courseId=1 studentId=1`，总评按新分数重算为 63.3 | PASS |
| 记录修改人 | 写入当前登录用户 | `updateBy=1`、`updaterName=系统管理员` | PASS |
| 移出 2 名已有成绩的学生 | 返回删除条数 | `data=2`，课程成绩从 8 条降为 6 条，名单降为 6 人 | PASS |
| 名单含不存在的学生 | 拒绝 | HTTP 400「学生名单中存在无效的学生，请刷新后重试」 | PASS |
| 名单含重复 id | 去重后成功 | HTTP 200，名单恢复 8 人且无重复键异常 | PASS |

### 2.3 数据一致性复核

| 项目 | 结果 |
| --- | --- |
| `sys_user` / `student` / `teacher` / `course` | 11 / 8 / 2 / 7（与基线一致） |
| `score` | 40 |
| `course_student` | 42 |
| 课程 1 名单 | `1,2,3,4,5,6,7,8` |
| 总评与 `round(平时*0.3+期末*0.7, 1)` 不一致的记录 | 0 |
| 测试产生的额外数据 | 0（验证数据已按原值还原，`update_by` 复位为 `NULL`） |

## 3. 未做的部分与原因

| 项目 | 说明 |
| --- | --- |
| 前端交互的浏览器验证 | 名单影响提示、二次确认、编辑态禁用下拉框通过生产构建与代码审查验证，交互验证留待 Stage 4 前端测试覆盖 |
| 成绩修改的完整审计表 | 本次只记录最后修改人，未建立逐次变更历史表；如需要完整追溯可在后续阶段增加操作日志表 |

## 4. 结论

```text
PASS
```

Stage 2 列出的 6 项全部完成，成绩计算公式与统计口径未变，演示数据在验证后已还原到基线状态。
