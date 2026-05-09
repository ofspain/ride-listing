-- Bio field for seller profile
ALTER TABLE users
    ADD COLUMN bio TEXT,
    ADD COLUMN seller_slug VARCHAR(200),
    ADD COLUMN seller_slug_updated_at TIMESTAMP;

-- Index for fast seller slug lookups
CREATE INDEX idx_users_seller_slug
    ON users(seller_slug)
    WHERE seller_slug IS NOT NULL;

-- Backfill seller_slug for existing DEALER users
UPDATE users
SET seller_slug =
    LOWER(
        REGEXP_REPLACE(
            REGEXP_REPLACE(
                first_name || '-' || last_name,
                '[^a-zA-Z0-9\s-]', '', 'g'
            ),
            '\s+', '-', 'g'
        )
    ) || '-' || LEFT(
        REPLACE(id::text, '-', ''), 6
    )
WHERE account_type = 'DEALER';
