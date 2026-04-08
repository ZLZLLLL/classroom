-- 补充历史库中缺失的中文备注
-- init.sql 已同步补齐，以下用于存量数据库升级

-- 表中文备注（便于在数据库工具中直接看到“教师表/学生表”等）
ALTER TABLE sys_teacher COMMENT = '教师表';
ALTER TABLE sys_student COMMENT = '学生表';
ALTER TABLE sys_admin COMMENT = '管理员表';
ALTER TABLE sys_announcement COMMENT = '系统公告表';
ALTER TABLE sys_class COMMENT = '班级表';

ALTER TABLE edu_course COMMENT = '课程表';
ALTER TABLE edu_course_class COMMENT = '课程-班级关联表';
ALTER TABLE edu_course_student COMMENT = '课程-学生成员表';
ALTER TABLE edu_attendance COMMENT = '签到记录表';
ALTER TABLE edu_attendance_activity COMMENT = '签到活动表';
ALTER TABLE edu_question COMMENT = '提问表';
ALTER TABLE edu_answer COMMENT = '回答表';
ALTER TABLE edu_points COMMENT = '积分记录表';
ALTER TABLE edu_like COMMENT = '点赞表';
ALTER TABLE edu_vote COMMENT = '课程投票表';
ALTER TABLE edu_vote_record COMMENT = '课程投票记录表';
ALTER TABLE edu_homework COMMENT = '作业表';
ALTER TABLE edu_homework_submit COMMENT = '作业提交表';
ALTER TABLE edu_exam COMMENT = '考试表';
ALTER TABLE edu_exam_question COMMENT = '考试题目表';
ALTER TABLE edu_exam_submit COMMENT = '考试提交表';
ALTER TABLE edu_exam_answer COMMENT = '考试答题表';
ALTER TABLE edu_exam_notice COMMENT = '考试通知表';
ALTER TABLE edu_file COMMENT = '文件表';

-- 字段中文备注（补齐历史缺失）
ALTER TABLE edu_attendance_activity
    MODIFY COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '签到活动ID';


