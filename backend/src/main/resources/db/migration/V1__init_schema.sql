-- V1：初始化表结构（幂等，已存在的表会被跳过）
-- 与 sql/student_score.sql 保持一致，后者用于手工重置演示库。

CREATE TABLE IF NOT EXISTS sys_user
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
CREATE TABLE IF NOT EXISTS teacher
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
CREATE TABLE IF NOT EXISTS class_info
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_name      VARCHAR(50) NOT NULL UNIQUE,
    major           VARCHAR(50),
    grade_year      INT         NOT NULL,
    head_teacher_id BIGINT,
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_teacher FOREIGN KEY (head_teacher_id) REFERENCES teacher (id) ON DELETE SET NULL
);
CREATE TABLE IF NOT EXISTS student
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
CREATE TABLE IF NOT EXISTS semester
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    semester_name VARCHAR(50) NOT NULL UNIQUE,
    start_date    DATE        NOT NULL,
    end_date      DATE        NOT NULL,
    is_current    TINYINT     NOT NULL DEFAULT 0,
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS course
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
CREATE TABLE IF NOT EXISTS course_student
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id   BIGINT   NOT NULL,
    student_id  BIGINT   NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_student (course_id, student_id),
    CONSTRAINT fk_cs_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS score
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
CREATE TABLE IF NOT EXISTS score_change_log
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
