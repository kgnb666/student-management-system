-- V3：为旧版本数据库补齐增量字段与审计表。
-- 说明：V1 使用 CREATE TABLE IF NOT EXISTS，对「已存在但结构较旧」的库不会补列，
-- 因此这里用 information_schema 判断后再执行 ALTER，保证在 MySQL 8 上是幂等的。
-- 全新库由 V1 建好全部字段，本迁移只做空操作。

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'token_version') = 0,
    'ALTER TABLE sys_user ADD COLUMN token_version INT NOT NULL DEFAULT 0',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'need_change_password') = 0,
    'ALTER TABLE sys_user ADD COLUMN need_change_password TINYINT NOT NULL DEFAULT 0',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'course' AND column_name = 'score_status') = 0,
    'ALTER TABLE course ADD COLUMN score_status TINYINT NOT NULL DEFAULT 0',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'course' AND column_name = 'submit_time') = 0,
    'ALTER TABLE course ADD COLUMN submit_time DATETIME NULL',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'score' AND column_name = 'update_by') = 0,
    'ALTER TABLE score ADD COLUMN update_by BIGINT NULL',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
