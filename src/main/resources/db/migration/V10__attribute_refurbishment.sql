-- V10: Attribute system refurbishment
-- 1. Multiple listing types per attribute
-- 2. Icon URL support
-- 3. Acceptable values list (predefined choices)

-- ============================================================
-- CHANGE 1: Multiple ListingTypes per attribute
-- ============================================================

-- Create join table for attribute-listing type relationship
CREATE TABLE attribute_listing_types (
    attribute_id UUID NOT NULL REFERENCES attribute_definitions(id) ON DELETE CASCADE,
    listing_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (attribute_id, listing_type)
);

-- Migrate existing data from single listing_type column
INSERT INTO attribute_listing_types (attribute_id, listing_type)
SELECT id, listing_type
FROM attribute_definitions
WHERE listing_type IS NOT NULL;

-- Drop the old listing_type column
ALTER TABLE attribute_definitions DROP COLUMN listing_type;

-- Drop the old index that was on the single listing_type column
DROP INDEX IF EXISTS idx_attribute_definition_listing_type;

-- ============================================================
-- CHANGE 2: Add icon_url to attributes
-- ============================================================

ALTER TABLE attribute_definitions ADD COLUMN icon_url VARCHAR(500);

-- ============================================================
-- CHANGE 3: Acceptable values list per attribute
-- ============================================================

CREATE TABLE attribute_acceptable_values (
    attribute_id UUID NOT NULL REFERENCES attribute_definitions(id) ON DELETE CASCADE,
    value VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_attribute_acceptable_values_attr ON attribute_acceptable_values(attribute_id);
