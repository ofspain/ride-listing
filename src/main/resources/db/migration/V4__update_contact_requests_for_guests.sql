-- Make buyer_id nullable to support anonymous inquiries
ALTER TABLE contact_requests ALTER COLUMN buyer_id DROP NOT NULL;

-- Add fields for guest/anonymous inquiries
ALTER TABLE contact_requests ADD COLUMN sender_name VARCHAR(100);
ALTER TABLE contact_requests ADD COLUMN sender_phone VARCHAR(20);

-- Add constraint: either buyer_id OR (sender_name AND sender_phone) must be provided
-- This is handled at application level for flexibility
