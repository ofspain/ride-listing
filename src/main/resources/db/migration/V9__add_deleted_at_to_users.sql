-- Add deleted_at column to users table for soft delete functionality
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;

-- Create index for efficient filtering of non-deleted users
CREATE INDEX idx_users_deleted_at ON users(deleted_at);

-- Create composite index for common query pattern (enabled + deleted_at)
CREATE INDEX idx_users_active ON users(enabled, deleted_at) WHERE enabled = true AND deleted_at IS NULL;
