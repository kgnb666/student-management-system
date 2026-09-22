# Stage 0 基线报告

执行日期：2026-09-20

项目目录：`D:\wkk\Student Management System`

本阶段只做基线确认，未修改任何业务代码、未改动数据库数据、未新增依赖。

## 1. 环境探测

| 项目 | 结果 | 说明 |
| --- | --- | --- |
| Node.js | PASS | v24.19.0 |
| npm | PASS | 11.17.0 |
| JDK 17 | PASS | `D:\tools\jdk\jdk-17.0.20.1+1`（PATH 中无 java，需显式指定） |
| Maven | PASS | `D:\wkk\AI-Incident-Commander\.tools\apache-maven-3.9.16\bin\mvn.cmd`（PATH 中无 mvn） |
| Maven 运行环境 | PASS | Maven 3.9.16 + Java 17.0.20.1（Eclipse Adoptium），默认 locale `zh_CN`、platform encoding `GBK` |
| MySQL80 服务 | PASS | 状态 RUNNING |
| MySQL 版本 | PASS | 8.0.46，`root/123456` 可连接 127.0.0.1:3306 |

说明：Java 与 Maven 都不在系统 PATH 中，后续阶段构建时需要显式使用上表路径，或先设置 `JAVA_HOME`。

## 2. 基线构建

### 后端

| 命令 | 结果 | 耗时 |
| --- | --- | --- |
| `mvn -B -DskipTests clean package` | BUILD SUCCESS | 5.7 s |

产物：`backend/target/student-score-1.0.0.jar`，27,874,439 字节（约 26.6 MB）。

### 前端

| 命令 | 结果 | 耗时 |
| --- | --- | --- |
| `npm run build` | 构建成功 | 8.6 s |

产物体积（优化前基线）：

| 文件 | 体积 | gzip |
| --- | ---: | ---: |
| `assets/index-*.js`（Element Plus 全量） | 1,058.91 kB | 348.17 kB |
| `assets/ScoreDistributionChart-*.js`（ECharts 全量） | 1,035.82 kB | 343.90 kB |
| `assets/index-*.css` | 361.90 kB | — |
| `assets/request-*.js` | 52.04 kB | 19.84 kB |

构建存在 `Some chunks are larger than 500 kB after minification` 警告，这是 Stage 3 的优化目标。

## 3. 数据基线

| 数据表 | 记录数 |
| --- | ---: |
| `sys_user` | 11 |
| `teacher` | 2 |
| `class_info` | 2 |
| `student` | 8 |
| `semester` | 2 |
| `course` | 7 |
| `course_student` | 42 |
| `score` | 40 |

与 Stage 3 验收报告记录的封版数据一致，数据库处于正常演示状态。

## 4. 当前已知风险点（留待后续阶段）

| 阶段 | 风险点 | 依据 |
| --- | --- | --- |
| Stage 1 | 停用账号/重置密码后旧 JWT 仍有效 | `JwtInterceptor` 只验签不查用户状态 |
| Stage 1 | 注册接口匿名开放且注册即生效 | `WebConfig` 放行 `/api/auth/register`、`/api/auth/classes` |
| Stage 1 | 登录无失败次数限制 | `AuthService.login` 无锁定逻辑 |
| Stage 1 | CORS 同时开启 credentials 与通配端口 | `WebConfig.addCorsMappings` |
| Stage 1 | 数据库密码与 JWT 密钥明文入库 | `application.yml`、`start.bat` |
| Stage 2 | 成绩更新可换课换人 | `ScoreService.update` |
| Stage 2 | 移出课程学生时静默删除成绩 | `CourseService.assignStudents` |
| Stage 3 | 前端首屏体积过大、列表无分页、统计在内存计算 | 构建产物、`ScoreService.buildStatistics` |
| Stage 4 | 无任何自动化测试、无接口文档、无健康检查 | `mvn test` 无测试源码 |
| Stage 5 | 启动与部署脚本硬编码环境信息 | `start.bat`、`deploy-student-management.sh` |

## 5. 结论

```text
PASS
```

环境可用、构建可通过、数据库处于演示基线状态，可以进入 Stage 1。
