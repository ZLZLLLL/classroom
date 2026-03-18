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
    INDEX idx_student_no (student_no),
    INDEX idx_class_id (class_id),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

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
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
('admin001', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '系统管理员', 3, 1);

-- 教师
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
('teacher001', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '张老师', 1, 1),
('teacher002', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '李老师', 1, 1);

-- 学生
INSERT INTO sys_user (username, password, real_name, student_no, role, class_id, status) VALUES
('student001', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '王小明', '2021001', 2, 1, 1),
('student002', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '李小红', '2021002', 2, 1, 1),
('student003', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '张小华', '2021003', 2, 2, 1),
('student004', '$2a$10$8jCU9yAHc.Ot75V3nUJzieU8q.uG6CBc7Ka.XxSFwM9hFNDuQ1dTO', '刘小丽', '2021004', 2, 2, 1);

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

