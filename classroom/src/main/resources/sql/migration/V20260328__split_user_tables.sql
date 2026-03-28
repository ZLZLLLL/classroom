-- 用户表拆分迁移脚本: sys_user -> sys_teacher/sys_student/sys_admin
-- 说明:
-- 1) 本脚本会保留历史 user_id，避免业务表中的 user_id/teacher_id/publisher_id 失效。
-- 2) 执行前请备份数据库；建议在维护窗口执行。

USE classroom;

CREATE TABLE IF NOT EXISTS sys_teacher (
    id BIGINT PRIMARY KEY COMMENT '教师ID(沿用原用户ID)',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    student_no VARCHAR(50) UNIQUE COMMENT '工号/登录号',
    avatar VARCHAR(500) COMMENT '头像URL',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

CREATE TABLE IF NOT EXISTS sys_student (
    id BIGINT PRIMARY KEY COMMENT '学生ID(沿用原用户ID)',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    student_no VARCHAR(50) UNIQUE COMMENT '学号',
    class_id BIGINT COMMENT '班级ID',
    avatar VARCHAR(500) COMMENT '头像URL',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_class_id (class_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

CREATE TABLE IF NOT EXISTS sys_admin (
    id BIGINT PRIMARY KEY COMMENT '管理员ID(沿用原用户ID)',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    student_no VARCHAR(50) UNIQUE COMMENT '管理员编号',
    avatar VARCHAR(500) COMMENT '头像URL',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 按角色回填数据，使用 INSERT IGNORE 便于重复执行
INSERT IGNORE INTO sys_teacher (id, username, password, real_name, student_no, avatar, phone, email, status, deleted, create_time, update_time)
SELECT id, username, password, real_name, student_no, avatar, phone, email, status, deleted, create_time, update_time
FROM sys_user
WHERE role = 1;

INSERT IGNORE INTO sys_student (id, username, password, real_name, student_no, class_id, avatar, phone, email, status, deleted, create_time, update_time)
SELECT id, username, password, real_name, student_no, class_id, avatar, phone, email, status, deleted, create_time, update_time
FROM sys_user
WHERE role = 2;

INSERT IGNORE INTO sys_admin (id, username, password, real_name, student_no, avatar, phone, email, status, deleted, create_time, update_time)
SELECT id, username, password, real_name, student_no, avatar, phone, email, status, deleted, create_time, update_time
FROM sys_user
WHERE role = 3;

DROP VIEW IF EXISTS sys_user;
CREATE VIEW sys_user AS
SELECT id, username, password, real_name, student_no, 1 AS role, NULL AS class_id, avatar, phone, email,
       status, deleted, create_time, update_time
FROM sys_teacher
UNION ALL
SELECT id, username, password, real_name, student_no, 2 AS role, class_id, avatar, phone, email,
       status, deleted, create_time, update_time
FROM sys_student
UNION ALL
SELECT id, username, password, real_name, student_no, 3 AS role, NULL AS class_id, avatar, phone, email,
       status, deleted, create_time, update_time
FROM sys_admin;

-- 如需彻底移除旧表，请确认新版本代码已发布并完成验证后手工执行:
-- RENAME TABLE sys_user TO sys_user_legacy_backup;

