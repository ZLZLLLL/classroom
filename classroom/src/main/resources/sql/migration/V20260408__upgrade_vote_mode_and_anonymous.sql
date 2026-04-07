-- 升级投票能力：支持匿名/实名、单选/多选
SET @has_vote_type = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_vote' AND COLUMN_NAME = 'type'
);
SET @sql = IF(@has_vote_type = 0,
    'ALTER TABLE edu_vote ADD COLUMN type TINYINT NOT NULL DEFAULT 1 COMMENT ''类型: 1-单选 2-多选'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_vote_anonymous = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_vote' AND COLUMN_NAME = 'anonymous'
);
SET @sql = IF(@has_vote_anonymous = 0,
    'ALTER TABLE edu_vote ADD COLUMN anonymous TINYINT NOT NULL DEFAULT 1 COMMENT ''是否匿名: 0-实名 1-匿名'' AFTER type',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_old_uk = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_vote_record' AND INDEX_NAME = 'uk_vote_user'
);
SET @sql = IF(@has_old_uk > 0,
    'ALTER TABLE edu_vote_record DROP INDEX uk_vote_user',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_new_uk = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_vote_record' AND INDEX_NAME = 'uk_vote_user_option'
);
SET @sql = IF(@has_new_uk = 0,
    'ALTER TABLE edu_vote_record ADD UNIQUE KEY uk_vote_user_option (vote_id, user_id, option_key)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

