-- Create makes table (manufacturer)
CREATE TABLE makes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_make_slug ON makes(slug);

-- Create vehicle_models table (model of a make)
CREATE TABLE vehicle_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    make_id UUID NOT NULL REFERENCES makes(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_vehicle_model_slug ON vehicle_models(slug);
CREATE INDEX idx_vehicle_model_make_id ON vehicle_models(make_id);

-- Create model_years table (year/edition of a model)
CREATE TABLE model_years (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(10) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    vehicle_model_id UUID NOT NULL REFERENCES vehicle_models(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_model_year_slug ON model_years(slug);
CREATE INDEX idx_model_year_vehicle_model_id ON model_years(vehicle_model_id);

-- Add categorization columns to listings
ALTER TABLE listings ADD COLUMN make_id UUID REFERENCES makes(id);
ALTER TABLE listings ADD COLUMN vehicle_model_id UUID REFERENCES vehicle_models(id);
ALTER TABLE listings ADD COLUMN model_year_id UUID REFERENCES model_years(id);

-- Create indexes on listings categorization columns
CREATE INDEX idx_listing_make_id ON listings(make_id);
CREATE INDEX idx_listing_vehicle_model_id ON listings(vehicle_model_id);
CREATE INDEX idx_listing_model_year_id ON listings(model_year_id);

-- Drop old string-based columns
ALTER TABLE listings DROP COLUMN IF EXISTS make;
ALTER TABLE listings DROP COLUMN IF EXISTS model;
ALTER TABLE listings DROP COLUMN IF EXISTS year;
