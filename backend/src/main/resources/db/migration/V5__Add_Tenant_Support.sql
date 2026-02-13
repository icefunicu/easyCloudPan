-- V5__Add_Tenant_Support.sql
-- 添加多租户支持

-- 1. 添加租户表
CREATE TABLE IF NOT EXISTS tenant_info (
    tenant_id VARCHAR(10) PRIMARY KEY,
    tenant_name VARCHAR(150) NOT NULL,
    tenant_code VARCHAR(50) UNIQUE NOT NULL,
    storage_quota BIGINT DEFAULT 10737418240,
    user_quota INTEGER DEFAULT 100,
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP
);

-- 2. 插入默认租户 (必须在添加外键之前)
INSERT INTO tenant_info (tenant_id, tenant_name, tenant_code, storage_quota, user_quota)
VALUES ('default', '默认租户', 'default', 107374182400, 1000)
ON CONFLICT (tenant_id) DO NOTHING;

-- 3. 为现有表添加 tenant_id 字段
ALTER TABLE user_info ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(10) DEFAULT 'default';
ALTER TABLE file_info ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(10) DEFAULT 'default';
ALTER TABLE file_share ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(10) DEFAULT 'default';

-- 4. 创建租户相关索引
CREATE INDEX IF NOT EXISTS idx_user_tenant ON user_info(tenant_id);
CREATE INDEX IF NOT EXISTS idx_file_tenant ON file_info(tenant_id);
CREATE INDEX IF NOT EXISTS idx_share_tenant ON file_share(tenant_id);

-- 5. 添加外键约束 (使用 DO 块检查约束是否存在)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_tenant') THEN
        ALTER TABLE user_info ADD CONSTRAINT fk_user_tenant 
            FOREIGN KEY (tenant_id) REFERENCES tenant_info(tenant_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_file_tenant') THEN
        ALTER TABLE file_info ADD CONSTRAINT fk_file_tenant 
            FOREIGN KEY (tenant_id) REFERENCES tenant_info(tenant_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_share_tenant') THEN
        ALTER TABLE file_share ADD CONSTRAINT fk_share_tenant 
            FOREIGN KEY (tenant_id) REFERENCES tenant_info(tenant_id);
    END IF;
END $$;
