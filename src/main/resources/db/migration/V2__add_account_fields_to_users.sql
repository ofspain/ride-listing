-- Add account-related fields to users table
ALTER TABLE users ADD COLUMN state VARCHAR(100);
ALTER TABLE users ADD COLUMN account_type VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL';

-- Index for filtering by state
CREATE INDEX idx_users_state ON users(state);
CREATE INDEX idx_users_account_type ON users(account_type);
