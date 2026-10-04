-- Repairs the original schema naming.
--
-- The original V2 unconditionally renamed a column ("descreption") that V1 never
-- created, so this migration could never succeed on a fresh database, and V3 then
-- inserted into "available_quantity" while V1 had created "availableQuantity".
--
-- V1 is left untouched so that already-migrated databases keep a valid checksum.
-- Every statement below is conditional, so this runs correctly against:
--   * a fresh database, where V1 already created "description"/"availableQuantity"
--   * a legacy database created before the typo was fixed
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'category'
          AND column_name = 'descreption'
    ) THEN
        ALTER TABLE category RENAME COLUMN descreption TO description;
    END IF;
END
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'product'
          AND column_name = 'descreption'
    ) THEN
        ALTER TABLE product RENAME COLUMN descreption TO description;
    END IF;

    -- Hibernate maps the field availableQuantity to available_quantity, which is
    -- what ddl-auto=validate expects and what V3 inserts into.
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'product'
          AND column_name = 'availablequantity'
    ) THEN
        ALTER TABLE product RENAME COLUMN availablequantity TO available_quantity;
    END IF;
END
$$;