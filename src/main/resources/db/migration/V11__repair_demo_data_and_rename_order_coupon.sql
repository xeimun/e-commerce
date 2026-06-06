update issued_coupons
set status = 'EXPIRED',
    updated_at = timestamp '2026-06-06 00:00:00'
where status = 'AVAILABLE'
  and exists (
      select 1
      from coupons
      where coupons.id = issued_coupons.coupon_id
        and name = '달빛 상점 아트북 4000원 할인'
        and type = 'PRODUCT'
        and discount_amount = 4000.00
        and target_product_id = 10001
        and created_at = timestamp '2026-06-06 00:00:00'
        and updated_at = timestamp '2026-06-06 00:00:00'
        and not exists (
            select 1
            from products
            where id = 10001
              and name = '달빛 상점 한정판 아트북'
              and price = 35000.00
              and status = 'ON_SALE'
              and content_title = '달빛 상점'
              and content_type = 'WEBTOON'
              and category = 'ARTBOOK'
              and description = '웹툰 달빛 상점 시즌 1의 콘셉트 아트와 작가 코멘터리를 담은 한정판 아트북'
        )
  );

update coupons
set expires_at = timestamp '2026-06-06 00:00:00',
    updated_at = timestamp '2026-06-06 00:00:00'
where name = '달빛 상점 아트북 4000원 할인'
  and type = 'PRODUCT'
  and discount_amount = 4000.00
  and target_product_id = 10001
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and exists (select 1 from issued_coupons where coupon_id = coupons.id)
  and not exists (
      select 1
      from products
      where id = 10001
        and name = '달빛 상점 한정판 아트북'
        and price = 35000.00
        and status = 'ON_SALE'
        and content_title = '달빛 상점'
        and content_type = 'WEBTOON'
        and category = 'ARTBOOK'
        and description = '웹툰 달빛 상점 시즌 1의 콘셉트 아트와 작가 코멘터리를 담은 한정판 아트북'
  );

delete from coupons
where name = '달빛 상점 아트북 4000원 할인'
  and type = 'PRODUCT'
  and discount_amount = 4000.00
  and target_product_id = 10001
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and not exists (select 1 from issued_coupons where coupon_id = coupons.id)
  and not exists (
      select 1
      from products
      where id = 10001
        and name = '달빛 상점 한정판 아트북'
        and price = 35000.00
        and status = 'ON_SALE'
        and content_title = '달빛 상점'
        and content_type = 'WEBTOON'
        and category = 'ARTBOOK'
        and description = '웹툰 달빛 상점 시즌 1의 콘셉트 아트와 작가 코멘터리를 담은 한정판 아트북'
  );

update issued_coupons
set status = 'EXPIRED',
    updated_at = timestamp '2026-06-06 00:00:00'
where status = 'AVAILABLE'
  and exists (
      select 1
      from coupons
      where coupons.id = issued_coupons.coupon_id
        and name = '라스트 오케스트라 OST 7000원 할인'
        and type = 'PRODUCT'
        and discount_amount = 7000.00
        and target_product_id = 10004
        and created_at = timestamp '2026-06-06 00:00:00'
        and updated_at = timestamp '2026-06-06 00:00:00'
        and not exists (
            select 1
            from products
            where id = 10004
              and name = '라스트 오케스트라 OST 패키지'
              and price = 42000.00
              and status = 'ON_SALE'
              and content_title = '라스트 오케스트라'
              and content_type = 'MUSIC'
              and category = 'OST'
              and description = 'OST 앨범, 미니 포토북, 넘버링 카드로 구성된 한정판 음악 패키지'
        )
  );

update coupons
set expires_at = timestamp '2026-06-06 00:00:00',
    updated_at = timestamp '2026-06-06 00:00:00'
where name = '라스트 오케스트라 OST 7000원 할인'
  and type = 'PRODUCT'
  and discount_amount = 7000.00
  and target_product_id = 10004
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and exists (select 1 from issued_coupons where coupon_id = coupons.id)
  and not exists (
      select 1
      from products
      where id = 10004
        and name = '라스트 오케스트라 OST 패키지'
        and price = 42000.00
        and status = 'ON_SALE'
        and content_title = '라스트 오케스트라'
        and content_type = 'MUSIC'
        and category = 'OST'
        and description = 'OST 앨범, 미니 포토북, 넘버링 카드로 구성된 한정판 음악 패키지'
  );

delete from coupons
where name = '라스트 오케스트라 OST 7000원 할인'
  and type = 'PRODUCT'
  and discount_amount = 7000.00
  and target_product_id = 10004
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and not exists (select 1 from issued_coupons where coupon_id = coupons.id)
  and not exists (
      select 1
      from products
      where id = 10004
        and name = '라스트 오케스트라 OST 패키지'
        and price = 42000.00
        and status = 'ON_SALE'
        and content_title = '라스트 오케스트라'
        and content_type = 'MUSIC'
        and category = 'OST'
        and description = 'OST 앨범, 미니 포토북, 넘버링 카드로 구성된 한정판 음악 패키지'
  );

delete from product_discounts
where product_id = 10001
  and name = '드롭 오픈 할인'
  and discount_amount = 3000.00
  and starts_at = timestamp '2026-06-01 00:00:00'
  and ends_at = timestamp '2027-12-31 23:59:59'
  and active = true
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and not exists (
      select 1
      from products
      where id = 10001
        and name = '달빛 상점 한정판 아트북'
        and price = 35000.00
        and status = 'ON_SALE'
        and content_title = '달빛 상점'
        and content_type = 'WEBTOON'
        and category = 'ARTBOOK'
        and description = '웹툰 달빛 상점 시즌 1의 콘셉트 아트와 작가 코멘터리를 담은 한정판 아트북'
  );

delete from product_discounts
where product_id = 10004
  and name = 'OST 예약 할인'
  and discount_amount = 5000.00
  and starts_at = timestamp '2026-06-01 00:00:00'
  and ends_at = timestamp '2027-12-31 23:59:59'
  and active = true
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and not exists (
      select 1
      from products
      where id = 10004
        and name = '라스트 오케스트라 OST 패키지'
        and price = 42000.00
        and status = 'ON_SALE'
        and content_title = '라스트 오케스트라'
        and content_type = 'MUSIC'
        and category = 'OST'
        and description = 'OST 앨범, 미니 포토북, 넘버링 카드로 구성된 한정판 음악 패키지'
  );

delete from stocks
where product_id = 10001
  and quantity = 120
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and not exists (
      select 1
      from products
      where id = 10001
        and name = '달빛 상점 한정판 아트북'
        and price = 35000.00
        and status = 'ON_SALE'
        and content_title = '달빛 상점'
        and content_type = 'WEBTOON'
        and category = 'ARTBOOK'
        and description = '웹툰 달빛 상점 시즌 1의 콘셉트 아트와 작가 코멘터리를 담은 한정판 아트북'
  );

delete from stocks
where product_id = 10002
  and quantity = 80
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and not exists (
      select 1
      from products
      where id = 10002
        and name = '검은별 기록관 포스터 세트'
        and price = 18000.00
        and status = 'ON_SALE'
        and content_title = '검은별 기록관'
        and content_type = 'WEB_NOVEL'
        and category = 'POSTER'
        and description = '다크 판타지 웹소설 검은별 기록관의 주요 장면을 담은 A3 포스터 3종 세트'
  );

delete from stocks
where product_id = 10003
  and quantity = 60
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and not exists (
      select 1
      from products
      where id = 10003
        and name = '푸른 사서의 방 아크릴 스탠드'
        and price = 22000.00
        and status = 'ON_SALE'
        and content_title = '푸른 사서의 방'
        and content_type = 'DRAMA'
        and category = 'ACRYLIC_STAND'
        and description = '드라마 푸른 사서의 방 주인공 일러스트를 활용한 데스크 아크릴 스탠드'
  );

delete from stocks
where product_id = 10004
  and quantity = 40
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and not exists (
      select 1
      from products
      where id = 10004
        and name = '라스트 오케스트라 OST 패키지'
        and price = 42000.00
        and status = 'ON_SALE'
        and content_title = '라스트 오케스트라'
        and content_type = 'MUSIC'
        and category = 'OST'
        and description = 'OST 앨범, 미니 포토북, 넘버링 카드로 구성된 한정판 음악 패키지'
  );

delete from stocks
where product_id = 10005
  and quantity = 150
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00'
  and not exists (
      select 1
      from products
      where id = 10005
        and name = '새벽의 문장 스티커팩'
        and price = 9000.00
        and status = 'ON_SALE'
        and content_title = '새벽의 문장'
        and content_type = 'MOVIE'
        and category = 'STICKER'
        and description = '영화 새벽의 문장 속 상징 문구와 오브젝트를 담은 다이어리 스티커팩'
  );

update coupons
set name = '드롭 기념 전체 상품 3000원 할인',
    updated_at = timestamp '2026-06-06 00:00:00'
where name = '첫 주문 전체 상품 3000원 할인'
  and type = 'ORDER'
  and discount_amount = 3000.00
  and target_product_id is null
  and expires_at = timestamp '2027-12-31 23:59:59'
  and created_at = timestamp '2026-06-06 00:00:00'
  and updated_at = timestamp '2026-06-06 00:00:00';
