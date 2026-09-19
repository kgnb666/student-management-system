# 学生成绩管理系统

一个基于 Vue 3 + Spring Boot + MySQL 的前后端分离课程设计项目，包含管理员、教师、学生三种角色。

## 功能

- 管理员：学生、教师、班级、课程、学期、成绩管理，维护课程学生名单。
- 教师：查看本人课程、维护课程学生成绩、查询成绩、查看课程和班级统计。
- 学生：查看个人信息、本人课程、按学期查询成绩和个人成绩统计。
- 统计指标：平均分、最高分、最低分、及格率、优秀率、成绩分布图。
- 总评成绩：平时成绩占 30%，期末成绩占 70%。

## 技术栈

- 后端：Spring Boot 3、MyBatis-Plus、JWT、MySQL 8
- 前端：Vue 3、Element Plus、Vue Router、Axios、ECharts
- 构建：Maven、Vite

## 目录结构

```text
Student Management System/
├─ backend/                 Spring Boot 后端
├─ frontend/                Vue 3 前端
├─ sql/student_score.sql    建表及演示数据
└─ README.md
```

## 运行步骤

### 一键启动

直接双击项目根目录下的 `start.bat`。脚本会自动：

1. 检查并启动 MySQL80 服务。
2. 首次运行时导入 `sql/student_score.sql`。
3. 启动后端和前端。
4. 打开 `http://localhost:5173`。

如果修改了 MySQL 密码，需要同步修改 `start.bat` 中的 `MYSQL_PWD`。

也可以按下面的步骤手动启动。

### 1. 初始化数据库

使用 MySQL 客户端执行：

```sql
source sql/student_score.sql;
```

脚本会创建 `student_score` 数据库，并写入演示数据。

### 2. 修改数据库配置

编辑 `backend/src/main/resources/application.yml`，将用户名和密码改为本机 MySQL 配置：

```yaml
spring:
  datasource:
    username: root
    password: 123456
```

### 3. 启动后端

在 `backend` 目录执行：

```bash
mvn spring-boot:run
```

如果本地环境执行 `spring-boot:run` 时出现 `ClassNotFoundException`，可以改用：

```bash
mvn -DskipTests package
java -jar target/student-score-1.0.0.jar
```

后端地址：`http://localhost:8080`

### 4. 启动前端

在 `frontend` 目录执行：

```bash
npm install
npm run dev
```

浏览器访问：`http://localhost:5173`

## 演示账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `admin123` |
| 教师 | `t001` 或 `t002` | `123456` |
| 学生 | `2023001` 至 `2023008` | `123456` |

新增学生或教师时，默认登录密码也是 `123456`。

## 主要接口

- 登录：`POST /api/auth/login`
- 管理员：`/api/admin/students`、`/api/admin/teachers`、`/api/admin/classes`、`/api/admin/courses`、`/api/admin/semesters`、`/api/admin/scores`
- 教师：`/api/teacher/courses`、`/api/teacher/scores`、`/api/teacher/courses/{id}/statistics`
- 学生：`/api/student/profile`、`/api/student/courses`、`/api/student/scores`、`/api/student/statistics`

除登录接口外，请求需要携带：

```text
Authorization: Bearer <token>
```

## 已完成的检查

- 前端生产构建：`npm run build`
- 后端编译打包：`mvn -DskipTests package`

## 注意事项

- 运行前先启动 MySQL 并导入 `sql/student_score.sql`。
- `application.yml` 中的数据库账号密码需要按本机环境修改。
- 新增学生和教师的默认密码都是 `123456`。
- 总评成绩按平时成绩 30%、期末成绩 70% 计算，保留 1 位小数。
