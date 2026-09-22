# Stage 3 性能优化报告

执行日期：2026-09-20

项目目录：`D:\wkk\Student Management System`

本阶段按《分阶段优化提示词》Stage 3 执行，功能与接口语义未改变，分数与统计结果与优化前逐条一致。

## 1. 改动清单

### 1.1 前端按需引入

| 文件 | 改动 |
| --- | --- |
| `package.json` | 新增开发依赖 `unplugin-vue-components`、`unplugin-auto-import` |
| `vite.config.js` | 接入两个插件与 `ElementPlusResolver`，模板中用到的组件与指令自动注册 |
| `src/main.js` | 移除 `app.use(ElementPlus)` 全量注册与 `zhCn` 引入，保留全量样式表 |
| `src/App.vue` | 使用 `el-config-provider` 提供中文语言包，保持原有交互文案 |
| `src/components/ScoreDistributionChart.vue` | 改为 `echarts/core` + 按需注册 `BarChart`、`GridComponent`、`TooltipComponent`、`CanvasRenderer` |

说明：样式仍统一引入 `element-plus/dist/index.css`，避免按组件注入样式出现遗漏或重复；
换取的收益是 JS 主包体积大幅下降，代价是样式表体积与优化前持平（361.9 kB）。

### 1.2 列表分页

| 文件 | 改动 |
| --- | --- |
| `config/MybatisPlusConfig.java` | 新增 MyBatis-Plus 分页插件（MySQL 方言） |
| `vo/PageResult.java` | 新增分页结果，含参数规范化（`size` 上限 100、默认 10） |
| `mapper/StudentMapper`、`CourseMapper`、`ScoreMapper` | 新增 `*Page` 方法，XML 抽出公共 `<sql>` 片段复用 |
| `service/StudentService`、`TeacherService`、`CourseService`、`ScoreService` | 新增 `listPaged` |
| `controller/AdminStudentController`、`AdminTeacherController`、`AdminCourseController`、`AdminScoreController` | 新增可选 `page`/`size` 参数 |
| `views/admin/StudentList.vue`、`TeacherList.vue`、`CourseList.vue`、`ScoreList.vue` | 接入 `el-pagination`（每页 10/20/50，切换条件自动回到第 1 页） |

兼容策略：不传 `page` 时接口仍返回原数组结构，旧调用方与页面选项加载（如课程弹窗的学生下拉）不受影响；
传 `page` 时返回 `{ records, total, current, size }`。

教师端 `/api/teacher/scores` 与学生端成绩列表未分页：前者当前前端未使用，后者的数据范围本身被限定为本人课程。

### 1.3 统计下推 SQL

| 文件 | 改动 |
| --- | --- |
| `vo/ScoreAggregateVO.java` | 新增聚合结果载体 |
| `mapper/ScoreMapper.java`、`mapper/ScoreMapper.xml` | 新增 `selectScoreAggregate`：一次查询返回平均/最高/最低/总数/及格数/优秀数与五档分布 |
| `service/ScoreService.java` | 课程统计与个人统计改为读取 SQL 聚合结果，删除内存 stream 统计逻辑 |

统计口径未变：及格 ≥ 60、优秀 ≥ 90、五档边界与原来一致，平均分保留 1 位小数，空集合返回全 0。

### 1.4 日志分级

| 文件 | 改动 |
| --- | --- |
| `application.yml` | `log-impl` 改为 `Slf4jImpl`，新增 `logging.level` 配置 |
| `application-dev.yml` | 新增：开发环境使用 `StdOutImpl` 并开启 `com.example.score` DEBUG |

## 2. 验证结果

### 2.1 构建与体积

| 命令 | 结果 |
| --- | --- |
| `mvn -B -DskipTests package` | BUILD SUCCESS（5.4 s） |
| `npm run build` | 构建成功（7.3 s） |

前端产物体积对比：

| 产物 | 优化前 | 优化后 |
| --- | ---: | ---: |
| 主包 chunk | `index-*.js` 1,058.91 kB（gzip 348.17 kB） | 最大 chunk `index-*.js` 150.96 kB（gzip 59.25 kB） |
| 图表 chunk | `ScoreDistributionChart-*.js` 1,035.82 kB（gzip 343.90 kB） | 456.19 kB（gzip 153.35 kB） |
| dist 总大小 | 约 2.6 MB | 1,575,891 字节（55 个文件） |
| 构建大包警告 | 有（>500 kB） | 无 |

### 2.2 分页验证

后端以 `SERVER_PORT=18811` 启动，连接真实 MySQL 8.0.46：

| 场景 | 预期 | 实测 | 结果 |
| --- | --- | --- | --- |
| `GET /admin/students`（无 page） | 返回数组 | `isArray=True`，8 条 | PASS |
| `GET /admin/students?page=2&size=3` | 分页对象 | `total=8 current=2 size=3 records=3`，首条 `2023004` | PASS |
| `GET /admin/teachers?page=1&size=1` | 分页对象 | `total=2 records=1` | PASS |
| `GET /admin/courses?page=1&size=2` | 分页对象 | `total=7 records=2` | PASS |
| `GET /admin/scores?page=3&size=7` | 分页对象 | `total=40 current=3 records=7` | PASS |
| `GET /admin/scores?keyword=2023001&page=1&size=5` | 分页 + 过滤生效 | `total=6 records=5`，全部为 2023001 | PASS |

### 2.3 统计正确性验证

| 统计口径 | MySQL 直接计算 | 接口返回 | 结果 |
| --- | --- | --- | --- |
| 课程 1（数据库原理）平均分 | 79.8 | 79.8 | PASS |
| 课程 1 最高/最低分 | 94.1 / 60.1 | 94.1 / 60.1 | PASS |
| 课程 1 五档分布 | 0/2/1/2/3（合计 8） | 0/2/1/2/3 | PASS |
| 课程 1 + 班级 1 | 86.1 | 86.1 | PASS |
| 学生 2023001 第二学期 | 87.0 | 87.0 | PASS |
| 学生 2023001 分布 | 0/0/0/3/1（合计 4） | 0/0/0/3/1 | PASS |
| 无成绩的学期 | 全 0 | `avg=max=min=pass=excellent=0`，分布全 0 | PASS |

其中课程 1 平均分 79.8 与 Stage 1 验收报告记录的历史值一致，说明统计口径没有发生偏移。

### 2.4 日志分级验证

启动日志中不再出现 `Logging initialized using 'org.apache.ibatis.logging.stdout.StdOutImpl'`，
未启用 `dev` profile 时控制台不再打印每条 SQL。

## 3. 未做的部分与原因

| 项目 | 说明 |
| --- | --- |
| 样式按组件注入 | 保留全量 CSS 是刻意取舍：可完全避免按需注入样式缺失，CSS 体积与优化前持平且不触发大包警告 |
| 教师/学生接口分页 | 数据范围受限，当前规模下增加分页只会增加复杂度 |
| 浏览器交互验证 | 分页组件与图表按需注册通过生产构建、接口验证与代码审查确认，交互细节留待 Stage 4 前端测试覆盖 |

## 4. 结论

```text
PASS
```

Stage 3 的 4 项优化全部落地：前端主包体积下降约 86%，图表包下降约 56%，大包警告消除；
列表支持分页且旧调用方式保持兼容；统计改为 SQL 聚合且结果与优化前完全一致。
