alter table issued_coupons
    add constraint uk_issued_coupons_coupon_customer unique (coupon_id, customer_id);
