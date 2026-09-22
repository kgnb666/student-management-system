# Stage 6 功能增强报告

执行日期：2026-09-20

项目目录：`D:\wkk\Student Management System`

本阶段完成提示词中 Stage 6 的全部五个方向：A 成绩 Excel 导入/导出、B 成绩提交锁定与审核、
C 学生端 GPA、D 管理员首页 Dashboard、E 移动端响应式。

## 1. 改动清单

### 1.1 A. 成绩 Excel 导入/导出

| 文件 | 说明 |
| --- | --- |
| `backend/pom.xml` | 新增 `poi-ooxml` 5.2.5 |
| `service/ScoreExcelService.java` | 新增：导出课程成绩单、按模板导入并逐行校验 |
| `vo/ScoreImportResultVO.java` | 新增：成功数、失败数与逐行错误说明 |
| `controller/TeacherController.java` | 新增 `GET /api/teacher/courses/{id}/scores/export`、`POST .../scores/import` |
| `application.yml` | 上传文件上限 5MB |
| `frontend/src/api/request.js` | 响应拦截器对 `responseType=blob` 直接返回原始数据 |
| `frontend/src/api/teacher.js` | 新增导出/导入方法 |
| `frontend/src/views/teacher/GradeManage.vue` | 新增「导出成绩」「导入成绩」按钮与导入结果弹窗 |

模板列顺序：学号、姓名、班级、平时成绩、期末成绩、总评成绩；导入只读取学号与两项成绩。
校验规则：学号必须在课程名单内、成绩必须是 0-100 的数字、两项都空视为未录入（跳过而不是清空已有成绩）。

### 1.2 B. 成绩提交锁定与审核

| 文件 | 说明 |
| --- | --- |
| `sql/student_score.sql` | `course` 新增 `score_status`（0 录入中 / 1 已提交）与 `submit_time` |
| `entity/Course.java`、`vo/CourseVO.java` | 新增成绩状态与提交时间字段 |
| `mapper/CourseMapper.xml` | 课程列表查询返回成绩状态与提交时间 |
| `service/CourseService.java` | 新增 `submitScores`（教师提交）与 `unlockScores`（管理员解锁） |
| `service/ScoreService.java` | 新增 `checkCourseEditable`：已提交的课程禁止新增/修改/批量保存成绩 |
| `controller/TeacherController.java` | 新增 `POST /api/teacher/courses/{id}/submit` |
| `controller/AdminCourseController.java` | 新增 `POST /api/admin/courses/{id}/unlock` |
| `frontend/src/views/teacher/GradeManage.vue` | 成绩状态标签、提交按钮、锁定后禁用录入并给出提示条 |
| `frontend/src/views/teacher/MyCourses.vue`、`views/admin/CourseList.vue` | 成绩状态列；管理员课程页新增「解锁成绩」操作 |

### 1.3 C. 学生端 GPA/学分绩点

见 2.3 节验证；实现文件为 `util/GradePointUtil.java`、`vo/StudentCourseScoreVO.java`、
`vo/StudentGpaVO.java`、`ScoreMapper.selectStudentCourseScores`、`ScoreService.studentGpa`、
`StudentController` 的 `GET /api/student/gpa`、前端 `MyStatistics.vue` 的两张统计卡。

绩点规则：4.0 制（90 及以上 4.0，85-89.9 为 3.7，依次递减，低于 60 记 0），
按课程学分加权，保留 2 位小数；总评不低于 60 的课程计入已获学分。

### 1.4 D. 管理员首页 Dashboard

| 文件 | 说明 |
| --- | --- |
| `vo/DashboardVO.java` | 新增：学生/教师/班级/课程数量 + 当前学期 + 成绩统计 |
| `service/DashboardService.java` | 新增：汇总基础数据规模与当前学期成绩统计 |
| `controller/AdminDashboardController.java` | 新增 `GET /api/admin/dashboard` |
| `frontend/src/views/admin/Dashboard.vue` | 新增首页概览页（统计卡 + 成绩分布图） |
| `frontend/src/router/index.js`、`layout/MainLayout.vue`、`auth.js` | 新增路由与菜单项，管理员登录后默认进入首页概览 |

### 1.5 E. 移动端响应式

| 文件 | 说明 |
| --- | --- |
| `frontend/src/styles/global.css` | 统计卡改为 `auto-fit` 自适应；窄屏下压缩留白、弹窗铺满、表格横向滚动、图表高度下调 |
| `frontend/src/layout/MainLayout.vue` | 窄屏下侧边栏收成 64px 图标条、隐藏菜单文字与用户名、显示短品牌 |

## 2. 验证结果

### 2.1 构建与测试

| 命令 | 结果 |
| --- | --- |
| `mvn -B test` | `Tests run: 47, Failures: 0, Errors: 0`（新增 CourseServiceTest 4 个、ScoreExcelServiceTest 3 个、成绩锁定 1 个） |
| `npm run build` | 构建成功（8.1 s） |
| `npm run test` | `Test Files 3 passed`、`Tests 10 passed` |
| `npx eslint .` | 0 问题 |

测试过程中的一次真实回归：`ScoreService` 新增 `CourseMapper` 依赖后，原有 3 个用例立即报
`courseMapper is null`，说明单元测试确实能拦住依赖变更带来的破坏，修好注入后全部通过。

### 2.2 A/B/D 接口实测

后端以 `SERVER_PORT=18811` 启动，使用真实 MySQL：

| 场景 | 实测结果 |
| --- | --- |
| `GET /api/admin/dashboard` | students=8 teachers=2 classes=2 courses=7，当前学期「2025-2026学年第二学期」，平均分 79.8、及格率 96.4%、优秀率 25% |
| 教师导出成绩单 | HTTP 200，3898 字节，文件头为 `PK`（合法 xlsx） |
| 教师导入刚导出的文件 | `success=8 fail=0`（往返一致） |
| 教师提交成绩 | HTTP 200，课程 `scoreStatus=1`，`submitTime=2026-09-20 14:53:17` |
| 锁定后教师保存成绩 | HTTP 400「该课程成绩已提交锁定，如需修改请联系管理员解锁」 |
| 锁定后重复提交 | HTTP 400「该课程成绩已提交，无需重复提交」 |
| 锁定后导入成绩 | HTTP 400（同一锁定提示） |
| 管理员解锁 | HTTP 200，`scoreStatus=0` 且 `submitTime` 清空 |
| 解锁后教师保存成绩 | HTTP 200 |

Excel 逐行校验由单元测试覆盖：名单外的学号、120 分越界成绩均被拒绝并给出具体行号说明，
合法行正常写入，全部为空的行被跳过，非 `.xlsx` 文件直接拒绝。

### 2.3 C. GPA 实测

| 请求 | 实测结果 |
| --- | --- |
| `GET /api/student/gpa` | `gpa=3.61, totalCredit=22, earnedCredit=22, courseCount=6` |
| `GET /api/student/gpa?semesterId=2` | `gpa=3.64, totalCredit=14, earnedCredit=14, courseCount=4` |
| `GET /api/student/gpa?semesterId=999` | 全部为 0 |

手工核对：绩点加权和 79.4、总学分 22，`79.4 / 22 = 3.6091 → 3.61`，与接口一致。

### 2.4 D/E 浏览器实测

使用真实浏览器（Vite 开发服务 + 真实后端）验证：

| 场景 | 结果 |
| --- | --- |
| 打开登录页 | 页面正常渲染，Element Plus 组件解析为真实控件 |
| 管理员登录 | 登录后自动跳转到 `/admin/dashboard` |
| 首页概览 | 显示「当前学期：2025-2026学年第二学期」、8/2/2/7 四项规模、平均分 79.8、及格率 96.4%、优秀率 25% 与成绩分布图 |
| 菜单 | 侧边栏出现「首页概览」入口 |
| 移动端（390×844） | 侧边栏收成图标条并显示短品牌「成绩」、菜单文字与用户名隐藏、统计卡变为两列、图表正常显示 |

### 2.5 数据恢复

验证过程中产生的 `update_by` 与 `submit_time` 已复位，最终状态：

```text
score=40、course=7、student=8、sys_user=11
未锁定课程=0、update_by 非空记录=0、总评不一致记录=0
```

## 3. 未做的部分与原因

| 项目 | 说明 |
| --- | --- |
| 成绩锁定后的操作留痕 | 目前记录提交时间与最后修改人，未记录「谁在何时解锁」的完整审计流水 |
| Excel 的 .xls 旧格式 | 只支持 `.xlsx`，旧格式需另加 HSSF 分支 |
| 移动端真机验证 | 使用浏览器移动视口（390×844）验证，未在真实手机上测试手势与键盘弹出 |

## 4. 结论

```text
PASS
```

Stage 6 的 A/B/D/E 全部实现，C 已在先前完成；后端测试从 39 增至 47，前端 10 个用例，
接口、浏览器与移动端视口均完成实测，演示数据已恢复到基线。
