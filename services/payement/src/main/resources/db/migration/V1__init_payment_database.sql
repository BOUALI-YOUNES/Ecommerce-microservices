create table if not exists payment(
    id integer not null primary key,
    amount numeric(38, 2),
    payment_methode varchar(32),
    order_id integer not null,
    created_at timestamp(6) not null,
    last_modified_date timestamp(6)
);

-- One payment per order, so a retried payment request cannot charge twice.
create unique index if not exists uq_payment_order_id on payment(order_id);

create sequence if not exists payment_seq increment by 50;