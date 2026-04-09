-- 为签到记录增加辅助签到操作人字段
ALTER TABLE edu_attendance
    ADD COLUMN operator_id BIGINT NULL COMMENT '辅助签到操作人ID（教师/管理员）' AFTER status;

