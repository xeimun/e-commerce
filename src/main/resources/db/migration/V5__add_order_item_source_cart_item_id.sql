alter table order_items
    add column source_cart_item_id bigint;

update order_items oi
set source_cart_item_id = (
    select ci.id
    from orders o
    join carts c on c.customer_id = o.customer_id
    join cart_items ci on ci.cart_id = c.id
        and ci.product_id = oi.product_id
    where o.id = oi.order_id
        and o.status = 'PAYMENT_PENDING'
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
    );

alter table order_items
    add constraint chk_order_items_source_cart_item_id_positive
    check (source_cart_item_id is null or source_cart_item_id > 0);
