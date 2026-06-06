insert into product_discounts (
    product_id, name, discount_amount, starts_at, ends_at, active, created_at, updated_at
)
select
    p.id,
    'Savoia S-21 드롭 할인',
    5000.00,
    timestamp '2026-06-07 00:00:00',
    timestamp '2027-12-31 23:59:59',
    true,
    timestamp '2026-06-07 12:00:00',
    timestamp '2026-06-07 12:00:00'
from products p
where p.id = 10002
  and p.name = '[Savoia] S-21 수상비행기 피규어'
  and not exists (select 1 from product_discounts where product_id = p.id);

update product_discounts
set name = '묘코 봉제 인형 드롭 할인',
    discount_amount = 3000.00,
    starts_at = timestamp '2026-06-07 00:00:00',
    ends_at = timestamp '2027-12-31 23:59:59',
    active = true,
    updated_at = timestamp '2026-06-07 12:00:00'
where product_id = 10004
  and name = '묘코 마스코트 출시 할인'
  and discount_amount = 5000.00
  and starts_at = timestamp '2026-06-01 00:00:00'
  and ends_at = timestamp '2027-12-31 23:59:59'
  and active = false
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-07 00:00:00'
  and exists (
      select 1
      from products
      where id = 10004
        and name = '[우주에서 온 묘코] 묘코 마스코트 봉제 인형'
  );

insert into product_discounts (
    product_id, name, discount_amount, starts_at, ends_at, active, created_at, updated_at
)
select
    p.id,
    '묘코 봉제 인형 드롭 할인',
    3000.00,
    timestamp '2026-06-07 00:00:00',
    timestamp '2027-12-31 23:59:59',
    true,
    timestamp '2026-06-07 12:00:00',
    timestamp '2026-06-07 12:00:00'
from products p
where p.id = 10004
  and p.name = '[우주에서 온 묘코] 묘코 마스코트 봉제 인형'
  and not exists (select 1 from product_discounts where product_id = p.id);

update issued_coupons
set status = 'EXPIRED',
    updated_at = timestamp '2026-06-07 12:00:00'
where status = 'AVAILABLE'
  and exists (
      select 1
      from coupons
      where coupons.id = issued_coupons.coupon_id
        and name = '드롭 기념 전체 상품 3000원 할인'
        and type = 'ORDER'
        and discount_amount = 3000.00
        and target_product_id is null
        and expires_at = timestamp '2027-12-31 23:59:59'
        and created_at = timestamp '2026-06-06 00:00:00'
        and updated_at = timestamp '2026-06-06 00:00:00'
  );

delete from coupons
where name = '드롭 기념 전체 상품 3000원 할인'
  and type = 'ORDER'
  and discount_amount = 3000.00
  and target_product_id is null
  and expires_at = timestamp '2027-12-31 23:59:59'
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and not exists (select 1 from issued_coupons where coupon_id = coupons.id);
