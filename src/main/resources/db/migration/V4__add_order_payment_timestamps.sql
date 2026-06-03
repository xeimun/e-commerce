alter table orders
    add column paid_at timestamp;

alter table orders
    add column payment_canceled_at timestamp;
