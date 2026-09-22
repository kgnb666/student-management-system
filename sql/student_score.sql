CREATE DATABASE IF NOT EXISTS student_score
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE student_score;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS score_change_log;
DROP TABLE IF EXISTS score;
DROP TABLE IF EXISTS course_student;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS semester;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS class_info;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS sys_user;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    real_name   VARCHAR(50)  NOT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    token_version INT        NOT NULL DEFAULT 0,
    need_change_password TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE teacher
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_no  VARCHAR(30) NOT NULL UNIQUE,
    name        VARCHAR(50) NOT NULL,
    gender      VARCHAR(10),
    phone       VARCHAR(20),
    title       VARCHAR(30),
    user_id     BIGINT      NOT NULL UNIQUE,
    status      TINYINT     NOT NULL DEFAULT 1,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
);

CREATE TABLE class_info
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_name      VARCHAR(50) NOT NULL UNIQUE,
    major           VARCHAR(50),
    grade_year      INT         NOT NULL,
    head_teacher_id BIGINT,
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_teacher FOREIGN KEY (head_teacher_id) REFERENCES teacher (id) ON DELETE SET NULL
);

CREATE TABLE student
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_no  VARCHAR(30) NOT NULL UNIQUE,
    name        VARCHAR(50) NOT NULL,
    gender      VARCHAR(10),
    birth_date  DATE,
    phone       VARCHAR(20),
    class_id    BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL UNIQUE,
    status      TINYINT     NOT NULL DEFAULT 1,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_class FOREIGN KEY (class_id) REFERENCES class_info (id),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
);

CREATE TABLE semester
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    semester_name VARCHAR(50) NOT NULL UNIQUE,
    start_date    DATE        NOT NULL,
    end_date      DATE        NOT NULL,
    is_current    TINYINT     NOT NULL DEFAULT 0,
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE course
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(30)   NOT NULL UNIQUE,
    course_name VARCHAR(80)   NOT NULL,
    credit      DECIMAL(3, 1) NOT NULL,
    hours       INT           NOT NULL,
    course_type VARCHAR(20)   NOT NULL,
    semester_id BIGINT        NOT NULL,
    teacher_id  BIGINT        NOT NULL,
    status      TINYINT       NOT NULL DEFAULT 1,
    score_status TINYINT      NOT NULL DEFAULT 0,
    submit_time DATETIME      NULL,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_semester FOREIGN KEY (semester_id) REFERENCES semester (id),
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id)
);

CREATE TABLE course_student
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id   BIGINT   NOT NULL,
    student_id  BIGINT   NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_student (course_id, student_id),
    CONSTRAINT fk_cs_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE
);

CREATE TABLE score
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id   BIGINT        NOT NULL,
    student_id  BIGINT        NOT NULL,
    usual_score DECIMAL(5, 1),
    exam_score  DECIMAL(5, 1),
    final_score DECIMAL(5, 1),
    update_by   BIGINT,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_student_score (course_id, student_id),
    CONSTRAINT fk_score_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_score_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE
);

CREATE TABLE score_change_log
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    score_id           BIGINT,
    course_id          BIGINT       NOT NULL,
    student_id         BIGINT,
    operator_user_id   BIGINT,
    operator_name      VARCHAR(50),
    operator_role      VARCHAR(20),
    action             VARCHAR(20)  NOT NULL,
    before_usual_score DECIMAL(5, 1),
    before_exam_score  DECIMAL(5, 1),
    before_final_score DECIMAL(5, 1),
    after_usual_score  DECIMAL(5, 1),
    after_exam_score   DECIMAL(5, 1),
    after_final_score  DECIMAL(5, 1),
    remark             VARCHAR(200),
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_score_log (score_id, create_time),
    KEY idx_course_log (course_id, create_time)
);

INSERT INTO sys_user (id, username, password, role, real_name, status)
VALUES (1, 'admin', '$2b$10$UcXZZXQBejOcyCHEFL/wwufBXu.Mk81wH0C93dP2MOv8EwATmWgfS', 'ADMIN', '系统管理员', 1),
       (2, 't001', '$2b$10$R/mcuE/Fjnn6obYGtyC0mO9N56b6qeWWOOvPoWMkxFknnLN6ThLui', 'TEACHER', '张明', 1),
       (3, 't002', '$2b$10$R/mcuE/Fjnn6obYGtyC0mO9N56b6qeWWOOvPoWMkxFknnLN6ThLui', 'TEACHER', '李静', 1),
       (4, '2023001', '$2b$10$R/mcuE/Fjnn6obYGtyC0mO9N56b6qeWWOOvPoWMkxFknnLN6ThLui', 'STUDENT', '王小明', 1),
       (5, '2023002', '$2b$10$R/mcuE/Fjnn6obYGtyC0mO9N56b6qeWWOOvPoWMkxFknnLN6ThLui', 'STUDENT', '李雨桐', 1),
       (6, '2023003', '$2b$10$R/mcuE/Fjnn6obYGtyC0mO9N56b6qeWWOOvPoWMkxFknnLN6ThLui', 'STUDENT', '张浩', 1),
       (7, '2023004', '$2b$10$R/mcuE/Fjnn6obYGtyC0mO9N56b6qeWWOOvPoWMkxFknnLN6ThLui', 'STUDENT', '陈思雨', 1),
       (8, '2023005', '$2b$10$R/mcuE/Fjnn6obYGtyC0mO9N56b6qeWWOOvPoWMkxFknnLN6ThLui', 'STUDENT', '刘洋', 1),
       (9, '2023006', '$2b$10$R/mcuE/Fjnn6obYGtyC0mO9N56b6qeWWOOvPoWMkxFknnLN6ThLui', 'STUDENT', '赵敏', 1),
       (10, '2023007', '$2b$10$R/mcuE/Fjnn6obYGtyC0mO9N56b6qeWWOOvPoWMkxFknnLN6ThLui', 'STUDENT', '周子涵', 1),
       (11, '2023008', '$2b$10$R/mcuE/Fjnn6obYGtyC0mO9N56b6qeWWOOvPoWMkxFknnLN6ThLui', 'STUDENT', '孙宇', 1);

INSERT INTO teacher (id, teacher_no, name, gender, phone, title, user_id, status)
VALUES (1, 't001', '张明', '男', '13900000001', '讲师', 2, 1),
       (2, 't002', '李静', '女', '13900000002', '副教授', 3, 1);

INSERT INTO class_info (id, class_name, major, grade_year, head_teacher_id)
VALUES (1, '软件工程2301班', '软件工程', 2023, 1),
       (2, '计算机科学2302班', '计算机科学与技术', 2023, 2);

INSERT INTO student (id, student_no, name, gender, birth_date, phone, class_id, user_id, status)
VALUES (1, '2023001', '王小明', '男', '2004-03-12', '13800000001', 1, 4, 1),
       (2, '2023002', '李雨桐', '女', '2004-08-20', '13800000002', 1, 5, 1),
       (3, '2023003', '张浩', '男', '2003-11-02', '13800000003', 1, 6, 1),
       (4, '2023004', '陈思雨', '女', '2004-05-18', '13800000004', 1, 7, 1),
       (5, '2023005', '刘洋', '男', '2003-09-25', '13800000005', 2, 8, 1),
       (6, '2023006', '赵敏', '女', '2004-01-30', '13800000006', 2, 9, 1),
       (7, '2023007', '周子涵', '女', '2004-06-11', '13800000007', 2, 10, 1),
       (8, '2023008', '孙宇', '男', '2003-12-16', '13800000008', 2, 11, 1);

INSERT INTO semester (id, semester_name, start_date, end_date, is_current)
VALUES (1, '2025-2026学年第一学期', '2025-09-01', '2026-01-15', 0),
       (2, '2025-2026学年第二学期', '2026-02-23', '2026-07-10', 1);

INSERT INTO course (id, course_code, course_name, credit, hours, course_type, semester_id, teacher_id, status)
VALUES (1, 'DB201', '数据库原理', 3.0, 48, '必修', 2, 1, 1),
       (2, 'CS202', 'Java程序设计', 4.0, 64, '必修', 2, 1, 1),
       (3, 'SE203', '软件工程', 3.0, 48, '必修', 2, 1, 1),
       (4, 'NE204', '计算机网络', 3.0, 48, '必修', 2, 2, 1),
       (5, 'DS205', '数据结构', 4.0, 64, '必修', 2, 2, 1),
       (6, 'MA101', '高等数学', 5.0, 80, '必修', 1, 2, 1),
       (7, 'EN101', '大学英语', 3.0, 48, '必修', 1, 1, 1);

INSERT INTO course_student (course_id, student_id)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
       (2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6),
       (3, 1), (3, 2), (3, 3), (3, 4),
       (4, 5), (4, 6), (4, 7), (4, 8),
       (5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 6), (5, 7), (5, 8),
       (6, 1), (6, 2), (6, 3), (6, 4), (6, 5), (6, 6), (6, 7), (6, 8),
       (7, 1), (7, 2), (7, 3), (7, 4);

INSERT INTO score (course_id, student_id, usual_score, exam_score)
VALUES (1, 1, 85, 88),
       (1, 2, 92, 95),
       (1, 3, 76, 72),
       (1, 4, 88, 91),
       (1, 5, 65, 58),
       (1, 6, 79, 83),
       (1, 7, 94, 90),
       (1, 8, 58, 62),
       (2, 1, 90, 86),
       (2, 2, 87, 93),
       (2, 3, 72, 68),
       (2, 4, 95, 92),
       (2, 5, 68, 74),
       (2, 6, 81, 79),
       (3, 1, 84, 80),
       (3, 2, 91, 89),
       (3, 3, 67, 71),
       (3, 4, 88, 85),
       (4, 5, 73, 69),
       (4, 6, 86, 90),
       (4, 7, 62, 66),
       (4, 8, 78, 82),
       (5, 1, 88, 92),
       (5, 2, 79, 84),
       (5, 3, 70, 65),
       (5, 4, 93, 96),
       (5, 5, 60, 55),
       (5, 6, 82, 77),
       (6, 1, 86, 84),
       (6, 2, 78, 81),
       (6, 3, 91, 88),
       (6, 4, 69, 72),
       (6, 5, 74, 70),
       (6, 6, 88, 85),
       (6, 7, 65, 61),
       (6, 8, 80, 76),
       (7, 1, 90, 93),
       (7, 2, 83, 86),
       (7, 3, 75, 79),
       (7, 4, 87, 90);

UPDATE score
SET final_score = ROUND(usual_score * 0.3 + exam_score * 0.7, 1);
