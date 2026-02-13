-- Add missing columns to file_share table
-- Created: 2026-02-13
-- Purpose: Add file_name, folder_type, file_category, file_type, file_cover columns to file_share table

ALTER TABLE file_share ADD COLUMN IF NOT EXISTS file_name VARCHAR(255);
ALTER TABLE file_share ADD COLUMN IF NOT EXISTS folder_type SMALLINT DEFAULT 0;
ALTER TABLE file_share ADD COLUMN IF NOT EXISTS file_category SMALLINT;
ALTER TABLE file_share ADD COLUMN IF NOT EXISTS file_type SMALLINT;
ALTER TABLE file_share ADD COLUMN IF NOT EXISTS file_cover VARCHAR(500);
