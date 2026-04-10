-- Add listing type discriminator
ALTER TABLE listings ADD COLUMN listing_type VARCHAR(20) NOT NULL DEFAULT 'VEHICLE';

-- Add state field for location filtering
ALTER TABLE listings ADD COLUMN state VARCHAR(100);

-- Vehicle-specific fields
ALTER TABLE listings ADD COLUMN vehicle_type VARCHAR(20);
ALTER TABLE listings ADD COLUMN make VARCHAR(100);

-- Part-specific fields
ALTER TABLE listings ADD COLUMN part_name VARCHAR(200);
ALTER TABLE listings ADD COLUMN part_category VARCHAR(100);
ALTER TABLE listings ADD COLUMN compatibility TEXT;

-- Rename brand to make (migrate existing data)
UPDATE listings SET make = brand WHERE brand IS NOT NULL;
ALTER TABLE listings DROP COLUMN brand;

-- Add indexes for new fields
CREATE INDEX idx_listings_listing_type ON listings(listing_type);
CREATE INDEX idx_listings_vehicle_type ON listings(vehicle_type);
CREATE INDEX idx_listings_state ON listings(state);
CREATE INDEX idx_listings_make ON listings(make);
CREATE INDEX idx_listings_part_category ON listings(part_category);
