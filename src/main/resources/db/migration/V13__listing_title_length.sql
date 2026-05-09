-- Enforce max title length at DB level
ALTER TABLE listings
    ALTER COLUMN title TYPE VARCHAR(150);

-- Log any existing titles outside the range (do not reject them — they are legacy data)
DO $$
DECLARE
    short_count INTEGER;
    long_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO short_count
    FROM listings
    WHERE LENGTH(title) < 45;

    SELECT COUNT(*) INTO long_count
    FROM listings
    WHERE LENGTH(title) > 150;

    RAISE NOTICE 'Titles under 45 chars: %', short_count;
    RAISE NOTICE 'Titles over 150 chars: %', long_count;
END $$;
