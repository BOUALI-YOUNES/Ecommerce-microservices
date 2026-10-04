-- The order service owns this schema. Note there is deliberately no foreign key on
-- customer_id: customers live in the customer service's MongoDB, so the reference is
-- a logical id only and cannot be constrained by Postgres.

create table if not exists customer_order(
    id integer not null primary key,
    reference varchar(255),
    total_amount numeric(38, 2),
    payment_method varchar(32),
    customer_id varchar(255),
    created_at timestamp(6) not null,
    last_modified_date timestamp(6)
);

create index if not exists idx_customer_order_customer_id on customer_order(customer_id);
create index if not exists idx_customer_order_reference on customer_order(reference);

create table if not exists order_line(
    id integer not null primary key,
    order_id integer not null,
    product_id integer,
    quantity double precision not null,
    constraint fk_order_line_order foreign key (order_id) references customer_order(id)
);

create index if not exists idx_order_line_order_id on order_line(order_id);

create sequence if not exists customer_order_seq increment by 50;
create sequence if not exists order_line_seq increment by 50;