alter table order_items
    add column source_cart_item_id bigint;

-- Backfill only cart items that existed when the pending order was created.
update order_items oi
set source_cart_item_id = (
    select ci.id
    from orders o
    join carts c on c.customer_id = o.customer_id
    join cart_items ci on ci.cart_id = c.id
        and ci.product_id = oi.product_id
    where o.id = oi.order_id
        and o.status = 'PAYMENT_PENDING'
        and ci.created_at <= o.created_at
)
where oi.source_cart_item_id is null
    and exists (
        select 1
        from orders o
        join carts c on c.customer_id = o.customer_id
        join cart_items ci on ci.cart_id = c.id
            and ci.product_id = oi.product_id
        where o.id = oi.order_id
            and o.status = 'PAYMENT_PENDING'
            and ci.created_at <= o.created_at
    );

alter table order_items
    add constraint chk_order_items_source_cart_item_id_positive
    check (source_cart_item_id is null or source_cart_item_id > 0);
