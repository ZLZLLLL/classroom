-- 补充历史库中缺失的中文备注
-- init.sql 已同步补齐，以下用于存量数据库升级

ALTER TABLE edu_attendance_activity
    MODIFY COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '签到活动ID';

