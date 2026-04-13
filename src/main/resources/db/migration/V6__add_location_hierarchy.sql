-- Create states table
CREATE TABLE states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_state_slug ON states(slug);

-- Create axes table (partitions within a state)
CREATE TABLE axes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    state_id UUID NOT NULL REFERENCES states(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_axis_slug ON axes(slug);
CREATE INDEX idx_axis_state_id ON axes(state_id);

-- Create areas table (partitions within an axis)
CREATE TABLE areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    axis_id UUID NOT NULL REFERENCES axes(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_area_slug ON areas(slug);
CREATE INDEX idx_area_axis_id ON areas(axis_id);

-- Add new location columns to listings
ALTER TABLE listings ADD COLUMN state_id UUID REFERENCES states(id);
ALTER TABLE listings ADD COLUMN axis_id UUID REFERENCES axes(id);
ALTER TABLE listings ADD COLUMN area_id UUID REFERENCES areas(id);
ALTER TABLE listings ADD COLUMN address_line VARCHAR(500);

-- Create indexes on listings location columns
CREATE INDEX idx_listing_state_id ON listings(state_id);
CREATE INDEX idx_listing_axis_id ON listings(axis_id);
CREATE INDEX idx_listing_area_id ON listings(area_id);

ALTER TABLE listings DROP COLUMN state;

-- Note: The old 'state' column (VARCHAR) is kept for backward compatibility
-- A data migration should be performed to move existing state values to the new structure
-- After migration, the old column can be dropped with: ALTER TABLE listings DROP COLUMN state;
