-- Order status, so a payment outcome can be reflected on the order and an unpaid
-- order is distinguishable from a paid one.
ALTER TABLE customer_order ADD COLUMN IF NOT EXISTS status varchar(32) not null default 'PENDING';

-- One business order per reference: this is what makes a retried POST /orders
-- idempotent instead of creating a duplicate order and decrementing stock twice.
CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_order_reference ON customer_order(reference);