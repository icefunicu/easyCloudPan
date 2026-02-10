-- 优化文件列表查询性能
-- 场景: 根据 user_id 和 file_pid (父文件夹) 查询文件列表是最高频操作
-- 添加联合索引 idx_user_pid

ALTER TABLE `file_info` ADD INDEX `idx_user_pid` (`user_id`, `file_pid`) USING BTREE;
