alter table coupons
    add column first_order_only boolean not null default false;

update coupons
set first_order_only = true
where name = '첫 주문 전체 상품 3000원 할인';

alter table coupons
    add constraint chk_coupons_first_order_only_type check (first_order_only = false or type = 'ORDER');
