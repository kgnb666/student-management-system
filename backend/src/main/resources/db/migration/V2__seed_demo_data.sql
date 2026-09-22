-- V2：写入演示数据（幂等，已存在的主键/唯一键会被忽略）
-- 与 sql/student_score.sql 中的数据保持一致。

INSERT IGNORE INTO sys_user (id, username, password, role, real_name, status)
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
INSERT IGNORE INTO teacher (id, teacher_no, name, gender, phone, title, user_id, status)
VALUES (1, 't001', '张明', '男', '13900000001', '讲师', 2, 1),
       (2, 't002', '李静', '女', '13900000002', '副教授', 3, 1);
INSERT IGNORE INTO class_info (id, class_name, major, grade_year, head_teacher_id)
VALUES (1, '软件工程2301班', '软件工程', 2023, 1),
       (2, '计算机科学2302班', '计算机科学与技术', 2023, 2);
INSERT IGNORE INTO student (id, student_no, name, gender, birth_date, phone, class_id, user_id, status)
VALUES (1, '2023001', '王小明', '男', '2004-03-12', '13800000001', 1, 4, 1),
       (2, '2023002', '李雨桐', '女', '2004-08-20', '13800000002', 1, 5, 1),
       (3, '2023003', '张浩', '男', '2003-11-02', '13800000003', 1, 6, 1),
       (4, '2023004', '陈思雨', '女', '2004-05-18', '13800000004', 1, 7, 1),
       (5, '2023005', '刘洋', '男', '2003-09-25', '13800000005', 2, 8, 1),
       (6, '2023006', '赵敏', '女', '2004-01-30', '13800000006', 2, 9, 1),
       (7, '2023007', '周子涵', '女', '2004-06-11', '13800000007', 2, 10, 1),
       (8, '2023008', '孙宇', '男', '2003-12-16', '13800000008', 2, 11, 1);
INSERT IGNORE INTO semester (id, semester_name, start_date, end_date, is_current)
VALUES (1, '2025-2026学年第一学期', '2025-09-01', '2026-01-15', 0),
       (2, '2025-2026学年第二学期', '2026-02-23', '2026-07-10', 1);
INSERT IGNORE INTO course (id, course_code, course_name, credit, hours, course_type, semester_id, teacher_id, status)
VALUES (1, 'DB201', '数据库原理', 3.0, 48, '必修', 2, 1, 1),
       (2, 'CS202', 'Java程序设计', 4.0, 64, '必修', 2, 1, 1),
       (3, 'SE203', '软件工程', 3.0, 48, '必修', 2, 1, 1),
       (4, 'NE204', '计算机网络', 3.0, 48, '必修', 2, 2, 1),
       (5, 'DS205', '数据结构', 4.0, 64, '必修', 2, 2, 1),
       (6, 'MA101', '高等数学', 5.0, 80, '必修', 1, 2, 1),
       (7, 'EN101', '大学英语', 3.0, 48, '必修', 1, 1, 1);
INSERT IGNORE INTO course_student (course_id, student_id)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
       (2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6),
       (3, 1), (3, 2), (3, 3), (3, 4),
       (4, 5), (4, 6), (4, 7), (4, 8),
       (5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 6), (5, 7), (5, 8),
       (6, 1), (6, 2), (6, 3), (6, 4), (6, 5), (6, 6), (6, 7), (6, 8),
       (7, 1), (7, 2), (7, 3), (7, 4);
INSERT IGNORE INTO score (course_id, student_id, usual_score, exam_score)
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
