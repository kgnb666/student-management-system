# Stage 9 成绩变更审计报告

执行日期：2026-09-20

项目目录：`D:\wkk\Student Management System`

本阶段对应《复查问题-分阶段优化提示词》阶段 9，覆盖复查报告第 5 项：
把「只记录最后修改人」升级为可追溯的成绩变更历史。

## 1. 改动清单

### 1.1 数据表

`sql/student_score.sql` 新增 `score_change_log`（只追加不改写），字段包括：

`id`、`score_id`、`course_id`、`student_id`、`operator_user_id`、`operator_name`、`operator_role`、
`action`、变更前后三项成绩（平时/期末/总评，各 3 个字段）、`remark`、`create_time`；
并建立 `(score_id, create_time)` 与 `(course_id, create_time)` 两个索引。

`action` 取值：`CREATE`、`UPDATE`、`DELETE`、`SUBMIT`、`UNLOCK`。

### 1.2 写入时机

| 文件 | 说明 |
| --- | --- |
| `service/ScoreChangeLogService.java` | 新增：记录单条成绩变更、课程级动作，并补全操作人姓名与角色 |
| `service/ScoreService.java` | 新增、修改、删除成绩时写日志；批量保存逐条判断，**值未变化不写** |
| `service/CourseService.java` | 提交锁定（SUBMIT）、管理员解锁（UNLOCK）、因移出名单连带删除成绩（DELETE）时写日志 |
| `controller/AdminScoreController.java`、`TeacherController.java`、`AdminCourseController.java` | 传入当前登录用户作为操作人 |

日志与主流程在同一事务内写入：主操作回滚时日志一并回滚。

### 1.3 查询接口

| 接口 | 说明 |
| --- | --- |
| `GET /api/admin/score-logs?courseId=&studentId=&page=&size=` | 管理员查询全部变更记录，遵循项目分页约定（不传 page 返回数组） |
| `GET /api/teacher/courses/{id}/score-logs` | 教师查询本人课程的变更记录，复用 `checkCourseOwner` 做归属校验 |

### 1.4 前端

| 文件 | 说明 |
| --- | --- |
| `components/ScoreChangeLogDrawer.vue` | 新增：变更记录抽屉（时间、操作人、操作类型、变更前后成绩、备注） |
| `views/admin/ScoreList.vue` | 每行新增「变更记录」入口，按课程 + 学生过滤 |
| `views/teacher/GradeManage.vue` | 工具栏新增「变更记录」入口，按课程过滤 |
| `api/admin.js`、`api/teacher.js` | 新增查询方法 |

## 2. 验证结果

### 2.1 构建与测试

| 命令 | 结果 |
| --- | --- |
| `mvn -B test` | `Tests run: 55, Failures: 0, Errors: 0` |
| `npx eslint .` | 0 问题 |
| `prettier --check` | All matched files use Prettier code style |
| `npm run test` | `Tests 10 passed` |
| `npm run build` | 构建成功（6.6 s） |

### 2.2 真实链路验证

后端以 `SERVER_PORT=18811` 启动，连接真实 MySQL，按顺序制造变更并查询日志：

| 操作 | 实测结果 |
| --- | --- |
| 管理员把 2023001 的课程 1 成绩从 85/88 改为 80/90 | HTTP 200 |
| 教师批量把成绩改回 85/88 | HTTP 200 |
| 教师提交课程 1 成绩 | HTTP 200 |
| 管理员解锁课程 1 | HTTP 200 |
| 把 2023007 加入课程 3 名单并新增成绩 | HTTP 200 |
| 删除该条成绩 | HTTP 200 |
| 教师重复保存相同成绩 | 日志数 6 → 6（**未变化的记录不写日志**） |

管理员查询 `GET /api/admin/score-logs?page=1&size=20` 返回 6 条：

```text
[DELETE] 学号 2023007 操作人=系统管理员 前=70/75/73.5 后=空
[CREATE] 学号 2023007 操作人=系统管理员 前=空 后=70/75/73.5
[UNLOCK] 课程级 操作人=系统管理员 备注=管理员解锁成绩
[SUBMIT] 课程级 操作人=张明 备注=教师提交成绩并锁定
[UPDATE] 学号 2023001 操作人=张明 前=80/90/87.0 后=85/88/87.1
[UPDATE] 学号 2023001 操作人=系统管理员 前=85/88/87.1 后=80/90/87.0
```

教师查询 `GET /api/teacher/courses/1/score-logs` 返回 4 条，只包含课程 1 的记录，
说明按课程归属过滤生效（课程 3 的两条记录不可见）。

### 2.3 数据恢复

```text
score_change_log 已清空（升级前无该表，基线为空表）
score=40、course_student=42、课程 3 名单恢复为 1,2,3,4
课程 1 的 2023001 成绩恢复为 85/88/87.1
总评不一致记录=0、锁定课程=0、update_by 非空记录=0
```

## 3. 未做的部分与原因

| 项目 | 说明 |
| --- | --- |
| 历史数据回溯 | 升级前已存在的成绩没有历史记录，属预期行为，已在 README 与页面空状态中说明 |
| 日志导出 | 本阶段只提供查询与分页展示，未提供导出 Excel 的能力 |
| 浏览器交互验证 | 抽屉组件通过构建、lint 与接口验证；真实浏览器验证在 Stage 6 已建立，本阶段未重复执行 |

## 4. 结论

```text
PASS
```

阶段 9 完成：成绩新增、修改、删除、提交锁定、解锁全部可追溯，未变更的重复保存不会产生噪音日志；
管理员与教师的查询权限按角色与课程归属隔离，演示数据已恢复到基线。
