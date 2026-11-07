-- Align an existing promotions table with the Promotion entity without dropping existing data.
-- Run this against the same PostgreSQL database configured for the application.
BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'promotions'
          AND column_name = 'banner_image_url'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'promotions'
          AND column_name = 'url_image_promotion'
    ) THEN
        ALTER TABLE promotions RENAME COLUMN banner_image_url TO url_image_promotion;
    END IF;
END $$;

ALTER TABLE promotions
    ADD COLUMN IF NOT EXISTS discount_value NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    ADD COLUMN IF NOT EXISTS end_date DATE NOT NULL DEFAULT DATE '9999-12-31',
    ADD COLUMN IF NOT EXISTS quantity INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS url_image_promotion VARCHAR(500);

COMMIT;
