alter table order_items
    add column source_cart_item_id bigint;

alter table order_items
    add constraint chk_order_items_source_cart_item_id_positive
    check (source_cart_item_id is null or source_cart_item_id > 0);
