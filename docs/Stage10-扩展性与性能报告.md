# Stage 10 扩展性与性能报告

执行日期：2026-09-20

项目目录：`D:\wkk\Student Management System`

本阶段对应《复查问题-分阶段优化提示词》阶段 10，覆盖复查报告第 7、8、9、10、11 项。

## 1. 改动清单

### 1.1 JWT 鉴权缓存（第 9 项）

| 文件 | 说明 |
| --- | --- |
| `service/UserAuthCache.java` | 新增：只缓存鉴权相关字段（角色、状态、token 版本、是否需改密），默认 30 秒 TTL，对外返回副本 |
| `interceptor/JwtInterceptor.java` | 改为通过缓存获取用户信息 |
| `service/AuthService.java`、`StudentService.java`、`TeacherService.java` | 改密、重置密码、启停用、创建、删除账号后主动清除缓存 |
| `application.yml` | 新增 `security.auth-cache-millis`（默认 30000） |

关键约束：缓存不能让「旧 token 立即失效」回退，因此所有账号写操作都显式 `evict`，
并有单测覆盖「命中缓存只查一次库 / 过期重新查库 / 主动失效后重新查库」。

### 1.2 前端样式按需加载（第 10 项）

| 文件 | 说明 |
| --- | --- |
| `vite.config.js` | `ElementPlusResolver` 改为 `importStyle: 'css'`；Vitest 增加 `server.deps.inline: ['element-plus']` |
| `src/main.js` | 移除 `import 'element-plus/dist/index.css'` 全量样式 |
| 12 个页面 / 组件 / 请求封装 | 删除手写的 `ElMessage`、`ElMessageBox` 引入，改由 `unplugin-auto-import` 注入（同时注入对应组件样式） |
| `eslint.config.js` | 声明 `ElMessage`、`ElMessageBox` 等自动引入的全局变量，避免误报 |

样式体积对比：

| 项目 | 优化前 | 优化后 |
| --- | ---: | ---: |
| 全量 CSS（单文件） | 361.9 kB | — |
| 按组件 CSS（31 个文件合计） | — | 201.9 kB（下降 44%） |
| dist 总体积 | 1,575,891 字节 | 1,475,693 字节 |

### 1.3 下拉框远程搜索（第 7 项）

| 文件 | 说明 |
| --- | --- |
| `views/admin/ScoreList.vue` | 课程筛选与新增成绩弹窗的课程下拉改为 `remote` 搜索，默认只取前 20 条；编辑时保证当前课程有回显选项 |
| `views/admin/CourseList.vue` | 学生名单改为打开弹窗时按需加载，进入课程管理页不再一次性拉取全部学生 |

**未做与原因**：课程学生名单的穿梭框仍需要全量学生数据（用于挑选未选学生），
本次只把它从"进入页面即加载"改为"打开弹窗才加载"；彻底解决需要改成服务端搜索式名单编辑，
已记录为后续项。

### 1.4 教师 / 学生列表分页（第 8 项）

| 文件 | 说明 |
| --- | --- |
| `controller/TeacherController.java` | `/api/teacher/scores` 支持可选 `page`/`size`，不传时保持原数组返回 |
| `controller/StudentController.java` | `/api/student/scores` 同上 |
| `views/student/MyScores.vue` | 学生成绩页接入分页组件（10/20/50） |

教师端成绩查询接口已支持分页，但当前教师界面使用的是「按课程展示名单」的视图（数据受课程规模限制），
因此未额外接入分页控件。

### 1.5 ECharts 体积评估（第 11 项）

现状（前序阶段已完成按需注册）：`ScoreDistributionChart` chunk 456.8 kB（gzip 153.4 kB），
只注册了 `BarChart`、`GridComponent`、`TooltipComponent`、`CanvasRenderer`。

评估结论：**保留 ECharts，不做替换**。理由：

1. 图表仅用于成绩分布柱状图，已无可裁剪的模块；
2. 换用自绘 SVG/CSS 图表可以省掉这 456 kB，但会失去 tooltip、坐标轴自适应、响应式 resize 等能力，
   需要重写组件并自行处理边界，收益（gzip 153 kB）与成本不匹配；
3. 若后续对首屏体积有更高要求，更划算的做法是把图表 chunk 与统计页一起做路由级懒加载（当前已是懒加载）。

## 2. 验证结果

### 2.1 构建与测试

| 命令 | 结果 |
| --- | --- |
| `mvn -B test` | `Tests run: 59, Failures: 0, Errors: 0`（新增 UserAuthCacheTest 4 个） |
| `npx eslint .` | 0 问题 |
| `prettier --check` | All matched files use Prettier code style |
| `npm run test` | `Test Files 3 passed`、`Tests 10 passed` |
| `npm run build` | 构建成功（7.1 s），无大包警告 |

### 2.2 真实接口验证

| 场景 | 实测结果 |
| --- | --- |
| 学生登录后访问个人信息 | HTTP 200 |
| 管理员立即重置该生密码后再用旧 token | HTTP 401「登录状态已失效」——**缓存未破坏立即失效语义** |
| `GET /api/admin/courses?page=1&size=20` | `total=7 records=7` |
| `GET /api/admin/courses?keyword=数据库&page=1&size=20` | `records=1`，课程名「数据库原理」 |
| `GET /api/student/scores?semesterId=2&page=1&size=2` | `total=4 records=2 current=1` |
| `GET /api/student/scores?semesterId=2`（不传分页） | 仍返回数组，`count=4` |
| `GET /api/student/scores?page=3&size=2` | `total=6 records=2` |
| `GET /api/teacher/scores?page=1&size=3` | `total=22 records=3` |
| `GET /api/teacher/scores`（不传分页） | 仍返回数组，`count=22` |

### 2.3 浏览器验证

使用真实浏览器（Vite 开发服务 + 真实后端）检查按需样式是否完整：

| 检查项 | 结果 |
| --- | --- |
| 开发服务日志 | 按组件加载 `base/card/form/form-item/input/button/message` 等样式模块 |
| 首页概览渲染 | 侧边栏菜单与图标、统计卡网格、标签、图表卡片、顶部「修改密码 / 退出登录」全部样式正常 |
| 数据 | 显示 8 学生 / 2 教师 / 2 班级 / 7 课程 / 0 待审核 / 平均分 79.8 / 及格率 96.4% / 优秀率 25% |

说明：首次截图出现空白是开发服务依赖预构建触发自动重载导致，刷新后渲染正常。

### 2.4 数据恢复

```text
score=40、score_change_log=0、need_change_password=1 的账号=0、update_by 非空记录=0
```

## 3. 未做的部分与原因

| 项目 | 说明 |
| --- | --- |
| 课程名单穿梭框的服务端搜索 | 需要重新设计名单编辑交互，已记录为后续项 |
| ECharts 替换 | 见 1.5 评估结论，收益与成本不匹配 |
| 教师端成绩分页控件 | 当前界面为课程名单视图，接口已支持分页 |
| 鉴权缓存跨实例失效 | 与登录限流一致为进程内实现，多实例需共享存储 |

## 4. 结论

```text
PASS
```

阶段 10 的 5 项全部处理完毕：鉴权缓存（经测试与实测确认不破坏立即失效语义）、
样式按需（CSS 下降 44% 且页面渲染正常）、下拉远程搜索、教师/学生成绩分页、ECharts 体积评估。
