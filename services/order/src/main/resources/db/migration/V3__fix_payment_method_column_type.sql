-- Aligns the payment_method column with the entity mapping.
--
-- Order.paymentMethod is @Enumerated(EnumType.STRING), so Hibernate expects
-- varchar. Databases created before that change hold a smallint ordinal plus a
-- generated enum check constraint. V1 uses "create table if not exists", so on a
-- legacy database it silently leaves the old table in place and the mismatch only
-- surfaces later as a schema validation failure at startup.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'customer_order'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'customer_order'
          AND column_name = 'payment_method'
          AND data_type <> 'character varying'
    ) THEN
        -- The ordinal constraint is invalid once the column stores names.
        ALTER TABLE customer_order DROP CONSTRAINT IF EXISTS customer_order_payment_method_check;

        -- Existing ordinal values become their index as text ("0", "1", ...). They
        -- will not match the enum constant names, so affected rows need manual
        -- correction; new rows are written correctly.
        ALTER TABLE customer_order
            ALTER COLUMN payment_method TYPE varchar(32)
            USING CASE payment_method
                WHEN 0 THEN 'PAYPAL'
                WHEN 1 THEN 'MASTER_CARD'
                WHEN 2 THEN 'VISA'
                WHEN 3 THEN 'BITCOIN'
                ELSE NULL
            END;
    END IF;
END
$$;