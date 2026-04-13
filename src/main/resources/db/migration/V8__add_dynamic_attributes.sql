-- Create attribute_definitions table
CREATE TABLE attribute_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    listing_type VARCHAR(50) NOT NULL,
    filterable BOOLEAN NOT NULL DEFAULT TRUE,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_attribute_definition_slug ON attribute_definitions(slug);
CREATE INDEX idx_attribute_definition_listing_type ON attribute_definitions(listing_type);
CREATE INDEX idx_attribute_definition_active ON attribute_definitions(active);

-- Create listing_attribute_values table
CREATE TABLE listing_attribute_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    attribute_id UUID NOT NULL REFERENCES attribute_definitions(id) ON DELETE CASCADE,
    value VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_listing_attribute UNIQUE (listing_id, attribute_id)
);

CREATE INDEX idx_listing_attribute_value ON listing_attribute_values(attribute_id, value);
CREATE INDEX idx_listing_attribute_listing ON listing_attribute_values(listing_id);
