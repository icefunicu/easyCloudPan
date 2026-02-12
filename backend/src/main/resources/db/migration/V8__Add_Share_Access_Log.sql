-- V8__Add_Share_Access_Log.sql
-- 添加分享访问日志表，用于记录所有文件分享访问

CREATE TABLE IF NOT EXISTS share_access_log (
    id BIGSERIAL PRIMARY KEY,
    share_id VARCHAR(20) NOT NULL,
    file_id VARCHAR(20),
    visitor_id VARCHAR(32),
    visitor_ip VARCHAR(50),
    visitor_user_agent VARCHAR(500),
    access_type VARCHAR(20) NOT NULL,
    access_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN DEFAULT true,
    error_message VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_share_access_log_share_id ON share_access_log(share_id);
CREATE INDEX IF NOT EXISTS idx_share_access_log_visitor_id ON share_access_log(visitor_id);
CREATE INDEX IF NOT EXISTS idx_share_access_log_access_time ON share_access_log(access_time);

COMMENT ON TABLE share_access_log IS '分享访问日志表';
COMMENT ON COLUMN share_access_log.share_id IS '分享ID';
COMMENT ON COLUMN share_access_log.file_id IS '访问的文件ID';
COMMENT ON COLUMN share_access_log.visitor_id IS '访问者用户ID（如果已登录）';
COMMENT ON COLUMN share_access_log.visitor_ip IS '访问者IP地址';
COMMENT ON COLUMN share_access_log.visitor_user_agent IS '访问者浏览器User-Agent';
COMMENT ON COLUMN share_access_log.access_type IS '访问类型：VIEW/ DOWNLOAD/ CHECK_CODE';
COMMENT ON COLUMN share_access_log.access_time IS '访问时间';
COMMENT ON COLUMN share_access_log.success IS '访问是否成功';
COMMENT ON COLUMN share_access_log.error_message IS '错误信息（如果失败）';
