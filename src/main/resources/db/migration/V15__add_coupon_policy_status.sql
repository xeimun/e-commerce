alter table coupons
    add column status varchar(20);

update coupons
set status = 'ACTIVE'
where status is null;

alter table coupons
    alter column status set not null;

alter table coupons
    add constraint chk_coupons_status check (status in ('ACTIVE', 'STOPPED'));

create index idx_coupons_status_expires_at on coupons (status, expires_at);

update coupons
set status = 'STOPPED',
    updated_at = timestamp '2026-06-07 12:00:00'
where name = '드롭 기념 전체 상품 3000원 할인'
  and type = 'ORDER'
  and discount_amount = 3000.00
  and target_product_id is null
  and created_at = timestamp '2026-06-06 00:00:00'
  and (
      (
          expires_at = timestamp '2027-12-31 23:59:59'
          and updated_at = timestamp '2026-06-06 00:00:00'
      )
      or (
          expires_at = timestamp '2026-06-07 00:00:00'
          and updated_at = timestamp '2026-06-07 12:00:00'
      )
  )
  and exists (select 1 from issued_coupons where coupon_id = coupons.id);
