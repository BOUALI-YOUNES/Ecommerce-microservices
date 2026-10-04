-- Postgres does not create an index for the referencing side of a foreign key,
-- so category lookups and any category join scanned the whole product table.
CREATE INDEX IF NOT EXISTS idx_product_category_id ON product (category_id);