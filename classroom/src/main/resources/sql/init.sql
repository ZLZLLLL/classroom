-- 课堂互动与问答系统 - 数据库初始化脚本
-- 说明：
-- 1) 本脚本包含【建库、建表、中文注释、初始化测试数据】。
-- 2) 若在已有数据库中执行，请注意：CREATE TABLE IF NOT EXISTS 不会覆盖已有表结构。
-- 3) 选课管理采用“成员快照表 edu_course_student”，用于稳定统计与避免学生换班导致课程列表漂移。

-- 创建数据库
CREATE DATABASE IF NOT EXISTS classroom DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE classroom;

-- ===========================
-- 用户表
-- ===========================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    student_no VARCHAR(50) COMMENT '学号(学生)',
    role TINYINT NOT NULL DEFAULT 2 COMMENT '角色: 1-教师 2-学生 3-管理员',
    class_id BIGINT COMMENT '班级ID',
    avatar VARCHAR(500) COMMENT '头像URL',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_username (username),
    UNIQUE KEY uk_student_no (student_no),
    INDEX idx_class_id (class_id),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ===========================
-- 系统公告表
-- ===========================
CREATE TABLE IF NOT EXISTS sys_announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容',
    publisher_id BIGINT NOT NULL COMMENT '发布人ID(管理员)',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_publisher_id (publisher_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- ===========================
-- 班级表
-- ===========================
CREATE TABLE IF NOT EXISTS sys_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
    name VARCHAR(100) NOT NULL COMMENT '班级名称',
    grade VARCHAR(20) COMMENT '年级',
    major VARCHAR(100) COMMENT '专业',
    description VARCHAR(500) COMMENT '班级描述',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ===========================
-- 课程表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    name VARCHAR(200) NOT NULL COMMENT '课程名称',
    description VARCHAR(1000) COMMENT '课程描述',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    cover_url VARCHAR(500) COMMENT '课程封面',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_teacher_id (teacher_id),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- ===========================
-- 课程-班级关联表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_course_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_course_class (course_id, class_id),
    INDEX idx_course_id (course_id),
    INDEX idx_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程-班级关联表';

-- ===========================
-- 课程-学生成员表（选课快照）
-- ===========================
-- 设计说明：
-- - 课程创建/关联班级后，将班级内学生写入该表；学生端“我的课程”以该表为准。
-- - status=0 表示从课程中移除（例如课程移除某个关联班级）。
CREATE TABLE IF NOT EXISTS edu_course_student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    user_id BIGINT NOT NULL COMMENT '学生ID',
    source_class_id BIGINT COMMENT '来源班级ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常 0-已移除',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',

    UNIQUE KEY uk_course_user (course_id, user_id),
    INDEX idx_course_id (course_id),
    INDEX idx_user_id (user_id),
    INDEX idx_source_class_id (source_class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程-学生成员表';

-- ===========================
-- 签到记录表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    sign_time DATETIME NOT NULL COMMENT '签到时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-成功 2-迟到 3-缺勤',
    latitude DECIMAL(10, 7) COMMENT '签到纬度',
    longitude DECIMAL(10, 7) COMMENT '签到经度',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_course_id (course_id),
    INDEX idx_user_id (user_id),
    INDEX idx_sign_time (sign_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录表';

-- ===========================
-- 签到活动表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_attendance_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL COMMENT '课程ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    duration INT DEFAULT 15 COMMENT '签到持续时间(分钟)',
    location VARCHAR(255) COMMENT '签到地点',
    class_ids VARCHAR(1000) COMMENT '目标班级ID列表(JSON)',
    status INT DEFAULT 1 COMMENT '1-进行中 2-已结束',
    deleted INT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到活动表';

-- ===========================
-- 提问表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提问ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    content TEXT NOT NULL COMMENT '提问内容',
    type TINYINT NOT NULL DEFAULT 1 COMMENT '类型: 1-单选 2-多选 3-填空 4-简答',
    options JSON COMMENT '选项JSON [{"label":"A","content":"..."}]',
    correct_answer VARCHAR(500) COMMENT '正确答案',
    explanation TEXT COMMENT '解析',
    points INT DEFAULT 5 COMMENT '积分',
    duration INT DEFAULT 0 COMMENT '答题时间(秒)，0表示不限制',
    class_ids VARCHAR(1000) COMMENT '目标班级ID列表(JSON)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-进行中 2-已结束',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提问表';

-- ===========================
-- 回答表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '回答ID',
    question_id BIGINT NOT NULL COMMENT '提问ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content TEXT NOT NULL COMMENT '回答内容',
    is_correct TINYINT DEFAULT 0 COMMENT '是否正确: 0-未评判 1-正确 2-错误',
    score INT DEFAULT 0 COMMENT '得分',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_question_id (question_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回答表';

-- ===========================
-- 积分记录表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    course_id BIGINT COMMENT '课程ID',
    type TINYINT NOT NULL COMMENT '类型: 1-签到 2-回答 3-点赞 4-作业 5-提问 6-手动加分',
    points INT NOT NULL COMMENT '积分(正数加分)',
    description VARCHAR(255) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_user_id (user_id),
    INDEX idx_course_id (course_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分记录表';

-- ===========================
-- 点赞表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    target_user_id BIGINT NOT NULL COMMENT '被点赞用户ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    type TINYINT NOT NULL COMMENT '类型: 1-回答点赞',
    target_id BIGINT NOT NULL COMMENT '目标ID(回答ID)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_user_target (user_id, type, target_id),
    INDEX idx_target_id (target_id),
    INDEX idx_target_user_id (target_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- ===========================
-- 作业表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_homework (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作业ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    title VARCHAR(200) NOT NULL COMMENT '作业标题',
    content TEXT COMMENT '作业内容',
    chapter VARCHAR(100) COMMENT '章节',
    deadline DATETIME COMMENT '截止时间',
    total_points INT DEFAULT 100 COMMENT '总分',
    class_ids VARCHAR(1000) COMMENT '目标班级ID列表(JSON)',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业表';

-- ===========================
-- 作业提交表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_homework_submit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提交ID',
    homework_id BIGINT NOT NULL COMMENT '作业ID',
    user_id BIGINT NOT NULL COMMENT '学生ID',
    content TEXT COMMENT '提交内容',
    file_path VARCHAR(500) COMMENT '文件路径',
    submit_time DATETIME COMMENT '提交时间',
    score INT DEFAULT 0 COMMENT '得分',
    feedback TEXT COMMENT '教师反馈',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-待批改 2-已批改 3-逾期',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_homework_id (homework_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交表';

-- ===========================
-- 考试表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_exam (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考试ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    title VARCHAR(200) NOT NULL COMMENT '考试标题',
    description TEXT COMMENT '考试说明',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '截止时间',
    duration INT DEFAULT 0 COMMENT '考试时长(分钟)，0表示不限制',
    total_points INT DEFAULT 100 COMMENT '总分',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-未发布 2-进行中 3-已结束',
    class_ids VARCHAR(1000) COMMENT '目标班级ID列表(JSON)',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试表';

-- ===========================
-- 考试题目表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_exam_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    type TINYINT NOT NULL COMMENT '类型: 1-单选 2-多选 3-填空 4-简答',
    content TEXT NOT NULL COMMENT '题目内容',
    options JSON COMMENT '选项JSON [{"label":"A","content":"..."}]',
    correct_answer VARCHAR(500) COMMENT '正确答案',
    explanation TEXT COMMENT '解析',
    points INT DEFAULT 0 COMMENT '分值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_exam_id (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试题目表';

-- ===========================
-- 考试提交表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_exam_submit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '提交ID',
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    user_id BIGINT NOT NULL COMMENT '学生ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-作答中 2-已提交 3-已批改',
    submit_time DATETIME COMMENT '提交时间',
    auto_submit TINYINT NOT NULL DEFAULT 0 COMMENT '是否自动提交',
    total_score INT DEFAULT 0 COMMENT '总分',
    objective_score INT DEFAULT 0 COMMENT '客观题得分',
    subjective_score INT DEFAULT 0 COMMENT '主观题得分',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_exam_user (exam_id, user_id),
    INDEX idx_exam_id (exam_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试提交表';

-- ===========================
-- 考试答题表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_exam_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '答题ID',
    submit_id BIGINT NOT NULL COMMENT '提交ID',
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    user_id BIGINT NOT NULL COMMENT '学生ID',
    content TEXT COMMENT '答案内容',
    is_correct TINYINT DEFAULT 0 COMMENT '是否正确: 0-未评判 1-正确 2-错误',
    score INT DEFAULT 0 COMMENT '得分',
    feedback TEXT COMMENT '教师反馈',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_submit_id (submit_id),
    INDEX idx_exam_id (exam_id),
    INDEX idx_question_id (question_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试答题表';

-- ===========================
-- 考试通知表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_exam_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    class_ids VARCHAR(1000) COMMENT '目标班级ID列表(JSON)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_exam_id (exam_id),
    INDEX idx_course_id (course_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试通知表';

-- ===========================
-- 文件表
-- ===========================
CREATE TABLE IF NOT EXISTS edu_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    course_id BIGINT COMMENT '课程ID',
    uploader_id BIGINT NOT NULL COMMENT '上传者ID',
    type TINYINT NOT NULL DEFAULT 1 COMMENT '类型: 1-课件 2-作业 3-共享资料',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    file_type VARCHAR(100) COMMENT '文件类型',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_course_id (course_id),
    INDEX idx_uploader_id (uploader_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- ===========================
-- 初始化测试数据
-- ===========================
-- 插入测试班级
INSERT INTO sys_class (name, grade, major, description) VALUES
('计算机2101', '2021', '计算机科学与技术', '计算机科学与技术专业21级1班'),
('计算机2102', '2021', '计算机科学与技术', '计算机科学与技术专业21级2班'),
('软件工程2101', '2021', '软件工程', '软件工程专业21级1班');

-- 插入测试用户 (密码均为 123456 的 BCrypt 加密)
-- 管理员
INSERT INTO sys_user (username, password, real_name, student_no, role, status) VALUES
('admin001', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '系统管理员', '22029990001', 3, 1);

-- 教师
INSERT INTO sys_user (username, password, real_name, student_no, role, status) VALUES
('teacher001', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '张老师', '22021010001', 1, 1),
('teacher002', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '李老师', '22021010002', 1, 1);

-- 学生
INSERT INTO sys_user (username, password, real_name, student_no, role, class_id, status) VALUES
('student001', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '王小明', '22021320421', 2, 1, 1),
('student002', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '李小红', '22021320422', 2, 1, 1),
('student003', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '张小华', '22021320521', 2, 2, 1),
('student004', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '刘小丽', '22021320522', 2, 2, 1);

-- 初始化系统公告
INSERT INTO sys_announcement (title, content, publisher_id) VALUES
('系统上线通知', '课堂互动与问答系统已上线，欢迎教师与学生使用。', 1);

-- 插入测试课程
INSERT INTO edu_course (name, description, teacher_id, cover_url) VALUES
('Java程序设计', 'Java编程语言基础课程', 1, 'https://picsum.photos/seed/java/400/300'),
('数据结构与算法', '数据结构与算法设计与实现', 1, 'https://picsum.photos/seed/ds/400/300'),
('数据库原理', '关系型数据库设计与SQL优化', 2, 'https://picsum.photos/seed/db/400/300');

-- 课程-班级关联
INSERT INTO edu_course_class (course_id, class_id) VALUES
(1, 1), (1, 2),
(2, 1), (2, 2),
(3, 1), (3, 3);

-- 初始化课程成员：把被关联班级中的学生写入课程-学生成员表（快照）
-- 注意：该语句可用于“历史数据回填/重新同步”。
INSERT INTO edu_course_student (course_id, user_id, source_class_id)
SELECT cc.course_id, u.id, u.class_id
FROM edu_course_class cc
JOIN sys_user u ON u.class_id = cc.class_id
WHERE u.role = 2
ON DUPLICATE KEY UPDATE status = 1;

-- ===========================
-- 扩充 Mock 数据（约 80 名学生 + 多业务场景）
-- ===========================

-- 扩充班级
INSERT INTO sys_class (name, grade, major, description) VALUES
('网络工程2101', '2021', '网络工程', '网络工程专业21级1班'),
('人工智能2101', '2021', '人工智能', '人工智能专业21级1班'),
('信息安全2101', '2021', '信息安全', '信息安全专业21级1班'),
('大数据2101', '2021', '数据科学与大数据技术', '大数据专业21级1班'),
('物联网2101', '2021', '物联网工程', '物联网工程专业21级1班');

-- 扩充教师
INSERT INTO sys_user (username, password, real_name, student_no, role, status) VALUES
('teacher003', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '赵老师', '22021010003', 1, 1),
('teacher004', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '孙老师', '22021010004', 1, 1),
('teacher005', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '周老师', '22021010005', 1, 1),
('teacher006', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '吴老师', '22021010006', 1, 1);

-- 批量补充学生：student005 ~ student080
INSERT INTO sys_user (username, password, real_name, student_no, role, class_id, status)
SELECT
    CONCAT('student', LPAD(seq.n, 3, '0')),
    '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO',
    CONCAT('学生', LPAD(seq.n, 3, '0')),
    CONCAT('2202133', LPAD(seq.n, 4, '0')),
    2,
    MOD(seq.n - 1, 8) + 1,
    1
FROM (
    SELECT ones.n + tens.n * 10 AS n
    FROM (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) ones
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) tens
) seq
WHERE seq.n BETWEEN 5 AND 80;

-- 扩充公告
INSERT INTO sys_announcement (title, content, publisher_id)
SELECT a.title, a.content, u.id
FROM (
    SELECT '教学活动提醒' AS title, '本周将集中开展签到与课堂提问演练，请同学们按时参加。' AS content
    UNION ALL
    SELECT '期中考试安排', '各课程期中考试时间已发布，请提前在系统查看考试通知。'
    UNION ALL
    SELECT '作业提交通知', '请同学们在截止时间前完成并提交课程作业。'
) a
JOIN sys_user u ON u.username = 'admin001';

-- 扩充课程
INSERT INTO edu_course (name, description, teacher_id, cover_url)
SELECT x.name, x.description, u.id, x.cover_url
FROM (
    SELECT '操作系统' AS name, '操作系统基本原理与实践' AS description, 'teacher003' AS teacher_username, 'https://picsum.photos/seed/os/400/300' AS cover_url
    UNION ALL
    SELECT '计算机网络', '计算机网络分层模型与协议实践', 'teacher004', 'https://picsum.photos/seed/net/400/300'
    UNION ALL
    SELECT '软件工程导论', '需求分析、设计与项目管理基础', 'teacher005', 'https://picsum.photos/seed/se/400/300'
    UNION ALL
    SELECT 'Python数据分析', 'Python在数据处理与可视化中的应用', 'teacher006', 'https://picsum.photos/seed/py/400/300'
    UNION ALL
    SELECT 'Web开发技术', '前后端协同开发基础与实践', 'teacher003', 'https://picsum.photos/seed/web/400/300'
) x
JOIN sys_user u ON u.username = x.teacher_username;

-- 扩充课程-班级关联
INSERT INTO edu_course_class (course_id, class_id)
SELECT c.id, cl.id
FROM (
    SELECT 'Java程序设计' AS course_name, '计算机2101' AS class_name
    UNION ALL SELECT 'Java程序设计', '软件工程2101'
    UNION ALL SELECT '数据结构与算法', '计算机2102'
    UNION ALL SELECT '数据库原理', '软件工程2101'
    UNION ALL SELECT '操作系统', '计算机2101'
    UNION ALL SELECT '操作系统', '网络工程2101'
    UNION ALL SELECT '计算机网络', '网络工程2101'
    UNION ALL SELECT '计算机网络', '信息安全2101'
    UNION ALL SELECT '软件工程导论', '软件工程2101'
    UNION ALL SELECT '软件工程导论', '人工智能2101'
    UNION ALL SELECT 'Python数据分析', '大数据2101'
    UNION ALL SELECT 'Python数据分析', '人工智能2101'
    UNION ALL SELECT 'Web开发技术', '计算机2102'
    UNION ALL SELECT 'Web开发技术', '物联网2101'
) m
JOIN edu_course c ON c.name = m.course_name
JOIN sys_class cl ON cl.name = m.class_name
ON DUPLICATE KEY UPDATE create_time = create_time;

-- 再次同步课程成员快照
INSERT INTO edu_course_student (course_id, user_id, source_class_id)
SELECT cc.course_id, u.id, u.class_id
FROM edu_course_class cc
JOIN sys_user u ON u.class_id = cc.class_id
WHERE u.role = 2
ON DUPLICATE KEY UPDATE status = 1;

-- 生成签到活动
INSERT INTO edu_attendance_activity (course_id, teacher_id, duration, location, class_ids, status)
SELECT
    c.id,
    c.teacher_id,
    20 + MOD(c.id, 15),
    CONCAT(c.name, '-教学楼A', MOD(c.id, 6) + 1, '0', MOD(c.id, 8) + 1),
    CONCAT('[', IFNULL(GROUP_CONCAT(cc.class_id ORDER BY cc.class_id), ''), ']'),
    2
FROM edu_course c
LEFT JOIN edu_course_class cc ON cc.course_id = c.id
GROUP BY c.id, c.teacher_id, c.name;

-- 生成签到记录
INSERT INTO edu_attendance (course_id, user_id, sign_time, status, latitude, longitude)
SELECT
    cs.course_id,
    cs.user_id,
    DATE_SUB(NOW(), INTERVAL MOD(cs.user_id * 3 + cs.course_id * 7, 21) DAY),
    CASE
        -- 签到结果分布：成功约 78%，迟到约 14%，缺勤约 8%
        WHEN MOD(cs.user_id * 5 + cs.course_id * 3, 100) < 78 THEN 1
        WHEN MOD(cs.user_id * 5 + cs.course_id * 3, 100) < 92 THEN 2
        ELSE 3
    END,
    31.2304000 + MOD(cs.user_id, 20) * 0.0001000,
    121.4737000 + MOD(cs.course_id, 20) * 0.0001000
FROM edu_course_student cs
WHERE cs.status = 1;

-- 生成课堂提问
INSERT INTO edu_question (course_id, teacher_id, content, type, options, correct_answer, explanation, points, duration, class_ids, status)
SELECT
    c.id,
    c.teacher_id,
    t.content,
    t.type,
    t.options,
    t.correct_answer,
    t.explanation,
    t.points,
    t.duration,
    CONCAT('[', IFNULL((SELECT GROUP_CONCAT(cc.class_id ORDER BY cc.class_id) FROM edu_course_class cc WHERE cc.course_id = c.id), ''), ']'),
    2
FROM (
    SELECT 'Java程序设计' AS course_name, 'Java中用于创建对象的关键字是？' AS content, 1 AS type,
           '[{"label":"A","content":"class"},{"label":"B","content":"new"},{"label":"C","content":"static"},{"label":"D","content":"void"}]' AS options,
           'B' AS correct_answer, 'new关键字用于实例化对象。' AS explanation, 5 AS points, 60 AS duration
    UNION ALL
    SELECT '数据结构与算法', '以下哪些属于线性结构？', 2,
           '[{"label":"A","content":"数组"},{"label":"B","content":"链表"},{"label":"C","content":"二叉树"},{"label":"D","content":"队列"}]',
           'A,B,D', '数组、链表、队列属于线性结构。', 8, 90
    UNION ALL
    SELECT '数据库原理', '请填写数据库事务的四个特性简称。', 3,
           NULL, 'ACID', '事务四大特性是ACID。', 6, 120
    UNION ALL
    SELECT '操作系统', '简述进程与线程的区别。', 4,
           NULL, NULL, '从资源分配、调度开销和并发粒度角度作答。', 10, 300
    UNION ALL
    SELECT '计算机网络', 'TCP三次握手的第二次报文是？', 1,
           '[{"label":"A","content":"SYN"},{"label":"B","content":"ACK"},{"label":"C","content":"SYN+ACK"},{"label":"D","content":"FIN"}]',
           'C', '第二次是SYN+ACK。', 5, 60
    UNION ALL
    SELECT 'Python数据分析', 'Pandas中常用的二维数据结构是？', 1,
           '[{"label":"A","content":"Series"},{"label":"B","content":"DataFrame"},{"label":"C","content":"ndarray"},{"label":"D","content":"tuple"}]',
           'B', '二维表格结构通常使用DataFrame。', 5, 60
) t
JOIN edu_course c ON c.name = t.course_name;

-- 生成学生回答
INSERT INTO edu_answer (question_id, user_id, content, is_correct, score)
SELECT
    q.id,
    cs.user_id,
    CASE
        WHEN q.type = 1 THEN CASE WHEN MOD(cs.user_id * 7 + q.id * 11, 100) < 72 THEN q.correct_answer ELSE 'A' END
        WHEN q.type = 2 THEN CASE WHEN MOD(cs.user_id * 7 + q.id * 11, 100) < 72 THEN q.correct_answer ELSE 'A,D' END
        WHEN q.type = 3 THEN CASE WHEN MOD(cs.user_id * 7 + q.id * 11, 100) < 72 THEN 'ACID' ELSE CONCAT('AICD-', MOD(cs.user_id, 5)) END
        ELSE CONCAT('学生', cs.user_id, '：进程是资源分配单位，线程是调度单位。')
    END,
    CASE
        WHEN q.type IN (1, 2, 3) THEN CASE WHEN MOD(cs.user_id * 7 + q.id * 11, 100) < 72 THEN 1 ELSE 2 END
        ELSE 0
    END,
    CASE
        WHEN q.type IN (1, 2, 3) THEN CASE WHEN MOD(cs.user_id * 7 + q.id * 11, 100) < 72 THEN q.points ELSE 0 END
        ELSE FLOOR(q.points * (0.55 + MOD(cs.user_id + q.id, 35) / 100))
    END
FROM edu_question q
JOIN edu_course_student cs ON cs.course_id = q.course_id AND cs.status = 1
WHERE MOD(cs.user_id + q.id * 2, 100) < 86;

-- 生成点赞记录
INSERT INTO edu_like (user_id, target_user_id, course_id, type, target_id, create_time)
SELECT
    cs.user_id,
    a.user_id,
    q.course_id,
    1,
    a.id,
    DATE_SUB(NOW(), INTERVAL MOD(cs.user_id + a.id, 7) DAY)
FROM edu_answer a
JOIN edu_question q ON q.id = a.question_id
JOIN edu_course_student cs ON cs.course_id = q.course_id AND cs.user_id <> a.user_id
WHERE MOD(cs.user_id + a.id, 17) = 0;

-- 生成积分记录：签到积分
INSERT INTO edu_points (user_id, course_id, type, points, description, create_time)
SELECT
    a.user_id,
    a.course_id,
    1,
    CASE WHEN a.status = 1 THEN 2 WHEN a.status = 2 THEN 1 ELSE 0 END,
    '签到积分',
    a.sign_time
FROM edu_attendance a
WHERE a.status IN (1, 2);

-- 生成积分记录：回答积分
INSERT INTO edu_points (user_id, course_id, type, points, description, create_time)
SELECT
    a.user_id,
    q.course_id,
    2,
    a.score,
    '课堂回答得分',
    a.create_time
FROM edu_answer a
JOIN edu_question q ON q.id = a.question_id
WHERE a.score > 0;

-- 生成积分记录：点赞积分
INSERT INTO edu_points (user_id, course_id, type, points, description, create_time)
SELECT
    l.target_user_id,
    l.course_id,
    3,
    1,
    '获得同学点赞',
    l.create_time
FROM edu_like l;

-- 生成作业
INSERT INTO edu_homework (course_id, teacher_id, title, content, chapter, deadline, total_points, class_ids, deleted)
SELECT
    c.id,
    c.teacher_id,
    CONCAT(c.name, ' - 第一阶段作业'),
    CONCAT('请完成', c.name, '章节练习并提交实验报告。'),
    '第1-3章',
    DATE_ADD(NOW(), INTERVAL 10 DAY),
    100,
    CONCAT('[', IFNULL((SELECT GROUP_CONCAT(cc.class_id ORDER BY cc.class_id) FROM edu_course_class cc WHERE cc.course_id = c.id), ''), ']'),
    0
FROM edu_course c;

-- 生成作业提交
INSERT INTO edu_homework_submit (homework_id, user_id, content, file_path, submit_time, score, feedback, status, deleted)
SELECT
    h.id,
    cs.user_id,
    CONCAT('这是作业提交内容，学生ID=', cs.user_id),
    CONCAT('/uploads/homework/', h.id, '/', cs.user_id, '.pdf'),
    CASE
        WHEN MOD(cs.user_id * 7 + h.id * 11, 100) >= 70 THEN DATE_ADD(h.deadline, INTERVAL MOD(cs.user_id, 3) + 1 DAY)
        ELSE DATE_SUB(h.deadline, INTERVAL MOD(cs.user_id, 4) DAY)
    END,
    CASE
        WHEN MOD(cs.user_id * 7 + h.id * 11, 100) < 55 THEN 68 + MOD(cs.user_id + h.id, 28)
        WHEN MOD(cs.user_id * 7 + h.id * 11, 100) < 70 THEN 55 + MOD(cs.user_id + h.id, 20)
        ELSE 0
    END,
    CASE WHEN MOD(cs.user_id * 7 + h.id * 11, 100) < 55 THEN '完成较好，建议补充实验截图与结论分析。' ELSE NULL END,
    CASE
        -- 作业分布：已批改约 55%，待批改约 15%，逾期约 15%，未提交约 15%
        WHEN MOD(cs.user_id * 7 + h.id * 11, 100) < 55 THEN 2
        WHEN MOD(cs.user_id * 7 + h.id * 11, 100) < 70 THEN 1
        ELSE 3
    END,
    0
FROM edu_homework h
JOIN edu_course_student cs ON cs.course_id = h.course_id AND cs.status = 1
WHERE MOD(cs.user_id * 7 + h.id * 11, 100) < 85;

-- 生成考试
INSERT INTO edu_exam (course_id, teacher_id, title, description, start_time, end_time, duration, total_points, status, class_ids, deleted)
SELECT
    c.id,
    c.teacher_id,
    CONCAT(c.name, '期中测试'),
    CONCAT(c.name, '阶段性测验，覆盖前半学期重点内容。'),
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    DATE_SUB(NOW(), INTERVAL 4 DAY),
    90,
    100,
    3,
    CONCAT('[', IFNULL((SELECT GROUP_CONCAT(cc.class_id ORDER BY cc.class_id) FROM edu_course_class cc WHERE cc.course_id = c.id), ''), ']'),
    0
FROM edu_course c;

-- 生成考试题目（每场 3 题）
INSERT INTO edu_exam_question (exam_id, type, content, options, correct_answer, explanation, points, sort_order)
SELECT
    e.id,
    q.type,
    CONCAT(e.title, ' - ', q.content),
    q.options,
    q.correct_answer,
    q.explanation,
    q.points,
    q.sort_order
FROM edu_exam e
JOIN (
    SELECT 1 AS type, '选择题1' AS content,
           '[{"label":"A","content":"选项A"},{"label":"B","content":"选项B"},{"label":"C","content":"选项C"},{"label":"D","content":"选项D"}]' AS options,
           'B' AS correct_answer, '标准答案为B。' AS explanation, 30 AS points, 1 AS sort_order
    UNION ALL
    SELECT 2, '多选题1',
           '[{"label":"A","content":"条件1"},{"label":"B","content":"条件2"},{"label":"C","content":"条件3"},{"label":"D","content":"条件4"}]',
           'A,C', '标准答案为A,C。', 30, 2
    UNION ALL
    SELECT 4, '简答题1', NULL, NULL, '围绕核心概念与实践应用展开即可。', 40, 3
) q;

-- 生成考试提交
INSERT INTO edu_exam_submit (exam_id, user_id, status, submit_time, auto_submit, total_score, objective_score, subjective_score, deleted)
SELECT
    e.id,
    cs.user_id,
    CASE WHEN MOD(cs.user_id * 13 + e.id * 17, 100) < 80 THEN 3 ELSE 2 END,
    DATE_SUB(e.end_time, INTERVAL MOD(cs.user_id, 60) MINUTE),
    CASE WHEN MOD(cs.user_id * 13 + e.id * 17, 100) BETWEEN 75 AND 89 THEN 1 ELSE 0 END,
    CASE
        WHEN MOD(cs.user_id * 13 + e.id * 17, 100) >= 80 THEN 0
        WHEN MOD(cs.user_id * 3 + e.id * 5, 100) < 75 THEN 60 + MOD(cs.user_id + e.id, 36)
        ELSE 45 + MOD(cs.user_id + e.id, 15)
    END,
    CASE
        WHEN MOD(cs.user_id * 13 + e.id * 17, 100) >= 80 THEN 0
        WHEN MOD(cs.user_id * 3 + e.id * 5, 100) < 75 THEN 34 + MOD(cs.user_id + e.id, 23)
        ELSE 20 + MOD(cs.user_id + e.id, 18)
    END,
    CASE
        WHEN MOD(cs.user_id * 13 + e.id * 17, 100) >= 80 THEN 0
        WHEN MOD(cs.user_id * 3 + e.id * 5, 100) < 75 THEN 24 + MOD(cs.user_id + e.id, 20)
        ELSE 15 + MOD(cs.user_id + e.id, 12)
    END,
    0
FROM edu_exam e
JOIN edu_course_student cs ON cs.course_id = e.course_id AND cs.status = 1
WHERE MOD(cs.user_id * 13 + e.id * 17, 100) < 90;

-- 生成考试答题
INSERT INTO edu_exam_answer (submit_id, exam_id, question_id, user_id, content, is_correct, score, feedback)
SELECT
    s.id,
    s.exam_id,
    q.id,
    s.user_id,
    CASE
        WHEN q.type = 1 THEN 'B'
        WHEN q.type = 2 THEN 'A,C'
        ELSE CONCAT('学生', s.user_id, '：这是简答题作答。')
    END,
    CASE
        WHEN q.type IN (1, 2) THEN CASE WHEN MOD(s.user_id + q.id, 5) = 0 THEN 2 ELSE 1 END
        ELSE 0
    END,
    CASE
        WHEN q.type IN (1, 2) THEN CASE WHEN MOD(s.user_id + q.id, 5) = 0 THEN 0 ELSE q.points END
        ELSE CASE WHEN MOD(s.user_id + q.id, 3) = 0 THEN q.points ELSE FLOOR(q.points * 0.7) END
    END,
    CASE
        WHEN q.type = 4 THEN '观点完整，建议补充案例。'
        ELSE NULL
    END
FROM edu_exam_submit s
JOIN edu_exam_question q ON q.exam_id = s.exam_id;

-- 生成考试通知
INSERT INTO edu_exam_notice (exam_id, course_id, title, content, class_ids)
SELECT
    e.id,
    e.course_id,
    CONCAT(e.title, '通知'),
    CONCAT('请同学们按时参加 ', e.title, '，并提前10分钟进入考场。'),
    e.class_ids
FROM edu_exam e;

-- 生成课程文件（教师课件）
INSERT INTO edu_file (course_id, uploader_id, type, file_name, file_path, file_size, file_type, deleted)
SELECT
    c.id,
    c.teacher_id,
    1,
    CONCAT(c.name, '-课件.pdf'),
    CONCAT('/uploads/courseware/', c.id, '/chapter1.pdf'),
    1024 * 1024 * (MOD(c.id, 8) + 1),
    'application/pdf',
    0
FROM edu_course c;

-- 生成课程文件（学生共享资料）
INSERT INTO edu_file (course_id, uploader_id, type, file_name, file_path, file_size, file_type, deleted)
SELECT
    cs.course_id,
    cs.user_id,
    3,
    CONCAT('共享资料-', cs.user_id, '.docx'),
    CONCAT('/uploads/share/', cs.course_id, '/', cs.user_id, '.docx'),
    1024 * (200 + MOD(cs.user_id, 300)),
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    0
FROM edu_course_student cs
WHERE cs.status = 1 AND MOD(cs.user_id + cs.course_id, 19) = 0;

