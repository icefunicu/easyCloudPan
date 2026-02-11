-- Improve cleanup/task/listing performance for file_info
-- Safe to rerun with IF NOT EXISTS

-- Speeds up recycle cleanup scans by del_flag + recovery_time
CREATE INDEX IF NOT EXISTS idx_file_info_del_flag_recovery_time
    ON file_info (del_flag, recovery_time);

-- Speeds up user folder listing ordered by last update time
CREATE INDEX IF NOT EXISTS idx_file_info_user_del_pid_last_update
    ON file_info (user_id, del_flag, file_pid, last_update_time DESC);

-- Speeds up category listing ordered by last update time
CREATE INDEX IF NOT EXISTS idx_file_info_user_del_category_last_update
    ON file_info (user_id, del_flag, file_category, last_update_time DESC);

-- Speeds up MD5 instant-upload lookup in a given status
CREATE INDEX IF NOT EXISTS idx_file_info_md5_status
    ON file_info (file_md5, status);
