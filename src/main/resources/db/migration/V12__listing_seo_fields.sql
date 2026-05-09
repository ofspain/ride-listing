-- Add listing_number as auto-increment sequence (not tied to UUID primary key)
CREATE SEQUENCE listing_number_seq
    START WITH 10000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Starting at 10000 gives listing numbers a consistent 5-digit length from the start
-- Looks professional: #10001, #10002

ALTER TABLE listings
    ADD COLUMN listing_number INTEGER UNIQUE DEFAULT nextval('listing_number_seq'),
    ADD COLUMN slug VARCHAR(300);

-- Backfill existing listings with numbers
-- (sequence handles new ones automatically)
UPDATE listings
SET listing_number = nextval('listing_number_seq')
WHERE listing_number IS NULL;

-- Make listing_number NOT NULL after backfill
ALTER TABLE listings
    ALTER COLUMN listing_number SET NOT NULL;

-- Backfill slugs for existing listings
-- Simple title-only slug for backfill
UPDATE listings
SET slug = LOWER(
    REGEXP_REPLACE(
        REGEXP_REPLACE(title, '[^a-zA-Z0-9\s-]', '', 'g'),
        '\s+', '-', 'g'
    )
)
WHERE slug IS NULL;

-- Make slug NOT NULL after backfill
ALTER TABLE listings
    ALTER COLUMN slug SET NOT NULL;

-- Indexes for fast lookups
CREATE INDEX idx_listings_listing_number ON listings(listing_number);

CREATE INDEX idx_listings_slug ON listings(slug);

-- Composite index for full URL resolution (listing_number + slug lookup)
CREATE INDEX idx_listings_number_slug ON listings(listing_number, slug);
