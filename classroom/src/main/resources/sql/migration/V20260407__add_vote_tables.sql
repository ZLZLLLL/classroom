-- 课程投票功能
CREATE TABLE IF NOT EXISTS edu_vote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '投票ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    title VARCHAR(200) NOT NULL COMMENT '投票标题',
    options JSON NOT NULL COMMENT '投票选项JSON',
    class_ids VARCHAR(1000) COMMENT '目标班级ID列表(JSON)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-进行中 2-已结束',
    type TINYINT NOT NULL DEFAULT 1 COMMENT '类型: 1-单选 2-多选',
    anonymous TINYINT NOT NULL DEFAULT 1 COMMENT '是否匿名: 0-实名 1-匿名',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程投票表';

CREATE TABLE IF NOT EXISTS edu_vote_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    vote_id BIGINT NOT NULL COMMENT '投票ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    user_id BIGINT NOT NULL COMMENT '学生ID',
    option_key VARCHAR(32) NOT NULL COMMENT '投票选项key',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_vote_user_option (vote_id, user_id, option_key),
    INDEX idx_vote_id (vote_id),
    INDEX idx_course_id (course_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程投票记录表';



