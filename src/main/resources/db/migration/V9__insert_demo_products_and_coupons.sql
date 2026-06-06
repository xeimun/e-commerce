insert into products (
    id, name, price, status, content_title, content_type, category, description, created_at, updated_at
)
select
    10001,
    '달빛 상점 한정판 아트북',
    35000.00,
    'ON_SALE',
    '달빛 상점',
    'WEBTOON',
    'ARTBOOK',
    '웹툰 달빛 상점 시즌 1의 콘셉트 아트와 작가 코멘터리를 담은 한정판 아트북',
    timestamp '2026-06-06 00:00:00',
    timestamp '2026-06-06 00:00:00'
where not exists (select 1 from products where id = 10001);

insert into products (
    id, name, price, status, content_title, content_type, category, description, created_at, updated_at
)
select
    10002,
    '검은별 기록관 포스터 세트',
    18000.00,
    'ON_SALE',
    '검은별 기록관',
    'WEB_NOVEL',
    'POSTER',
    '다크 판타지 웹소설 검은별 기록관의 주요 장면을 담은 A3 포스터 3종 세트',
    timestamp '2026-06-06 00:00:00',
    timestamp '2026-06-06 00:00:00'
where not exists (select 1 from products where id = 10002);

insert into products (
    id, name, price, status, content_title, content_type, category, description, created_at, updated_at
)
select
    10003,
    '푸른 사서의 방 아크릴 스탠드',
    22000.00,
    'ON_SALE',
    '푸른 사서의 방',
    'DRAMA',
    'ACRYLIC_STAND',
    '드라마 푸른 사서의 방 주인공 일러스트를 활용한 데스크 아크릴 스탠드',
    timestamp '2026-06-06 00:00:00',
    timestamp '2026-06-06 00:00:00'
where not exists (select 1 from products where id = 10003);

insert into products (
    id, name, price, status, content_title, content_type, category, description, created_at, updated_at
)
select
    10004,
    '라스트 오케스트라 OST 패키지',
    42000.00,
    'ON_SALE',
    '라스트 오케스트라',
    'MUSIC',
    'OST',
    'OST 앨범, 미니 포토북, 넘버링 카드로 구성된 한정판 음악 패키지',
    timestamp '2026-06-06 00:00:00',
    timestamp '2026-06-06 00:00:00'
where not exists (select 1 from products where id = 10004);

insert into products (
    id, name, price, status, content_title, content_type, category, description, created_at, updated_at
)
select
    10005,
    '새벽의 문장 스티커팩',
    9000.00,
    'ON_SALE',
    '새벽의 문장',
    'MOVIE',
    'STICKER',
    '영화 새벽의 문장 속 상징 문구와 오브젝트를 담은 다이어리 스티커팩',
    timestamp '2026-06-06 00:00:00',
    timestamp '2026-06-06 00:00:00'
where not exists (select 1 from products where id = 10005);

insert into stocks (product_id, quantity, created_at, updated_at)
select p.id, 120, timestamp '2026-06-06 00:00:00', timestamp '2026-06-06 00:00:00'
from products p
where p.id = 10001
  and p.name = '달빛 상점 한정판 아트북'
  and p.price = 35000.00
  and p.status = 'ON_SALE'
  and p.content_title = '달빛 상점'
  and p.content_type = 'WEBTOON'
  and p.category = 'ARTBOOK'
  and p.description = '웹툰 달빛 상점 시즌 1의 콘셉트 아트와 작가 코멘터리를 담은 한정판 아트북'
  and not exists (select 1 from stocks where product_id = p.id);

insert into stocks (product_id, quantity, created_at, updated_at)
select p.id, 80, timestamp '2026-06-06 00:00:00', timestamp '2026-06-06 00:00:00'
from products p
where p.id = 10002
  and p.name = '검은별 기록관 포스터 세트'
  and p.price = 18000.00
  and p.status = 'ON_SALE'
  and p.content_title = '검은별 기록관'
  and p.content_type = 'WEB_NOVEL'
  and p.category = 'POSTER'
  and p.description = '다크 판타지 웹소설 검은별 기록관의 주요 장면을 담은 A3 포스터 3종 세트'
  and not exists (select 1 from stocks where product_id = p.id);

insert into stocks (product_id, quantity, created_at, updated_at)
select p.id, 60, timestamp '2026-06-06 00:00:00', timestamp '2026-06-06 00:00:00'
from products p
where p.id = 10003
  and p.name = '푸른 사서의 방 아크릴 스탠드'
  and p.price = 22000.00
  and p.status = 'ON_SALE'
  and p.content_title = '푸른 사서의 방'
  and p.content_type = 'DRAMA'
  and p.category = 'ACRYLIC_STAND'
  and p.description = '드라마 푸른 사서의 방 주인공 일러스트를 활용한 데스크 아크릴 스탠드'
  and not exists (select 1 from stocks where product_id = p.id);

insert into stocks (product_id, quantity, created_at, updated_at)
select p.id, 40, timestamp '2026-06-06 00:00:00', timestamp '2026-06-06 00:00:00'
from products p
where p.id = 10004
  and p.name = '라스트 오케스트라 OST 패키지'
  and p.price = 42000.00
  and p.status = 'ON_SALE'
  and p.content_title = '라스트 오케스트라'
  and p.content_type = 'MUSIC'
  and p.category = 'OST'
  and p.description = 'OST 앨범, 미니 포토북, 넘버링 카드로 구성된 한정판 음악 패키지'
  and not exists (select 1 from stocks where product_id = p.id);

insert into stocks (product_id, quantity, created_at, updated_at)
select p.id, 150, timestamp '2026-06-06 00:00:00', timestamp '2026-06-06 00:00:00'
from products p
where p.id = 10005
  and p.name = '새벽의 문장 스티커팩'
  and p.price = 9000.00
  and p.status = 'ON_SALE'
  and p.content_title = '새벽의 문장'
  and p.content_type = 'MOVIE'
  and p.category = 'STICKER'
  and p.description = '영화 새벽의 문장 속 상징 문구와 오브젝트를 담은 다이어리 스티커팩'
  and not exists (select 1 from stocks where product_id = p.id);

insert into product_discounts (
    product_id, name, discount_amount, starts_at, ends_at, active, created_at, updated_at
)
select
    p.id,
    '드롭 오픈 할인',
    3000.00,
    timestamp '2026-06-01 00:00:00',
    timestamp '2027-12-31 23:59:59',
    true,
    timestamp '2026-06-06 00:00:00',
    timestamp '2026-06-06 00:00:00'
from products p
where p.id = 10001
  and p.name = '달빛 상점 한정판 아트북'
  and p.price = 35000.00
  and p.status = 'ON_SALE'
  and p.content_title = '달빛 상점'
  and p.content_type = 'WEBTOON'
  and p.category = 'ARTBOOK'
  and p.description = '웹툰 달빛 상점 시즌 1의 콘셉트 아트와 작가 코멘터리를 담은 한정판 아트북'
  and not exists (select 1 from product_discounts where product_id = p.id);

insert into product_discounts (
    product_id, name, discount_amount, starts_at, ends_at, active, created_at, updated_at
)
select
    p.id,
    'OST 예약 할인',
    5000.00,
    timestamp '2026-06-01 00:00:00',
    timestamp '2027-12-31 23:59:59',
    true,
    timestamp '2026-06-06 00:00:00',
    timestamp '2026-06-06 00:00:00'
from products p
where p.id = 10004
  and p.name = '라스트 오케스트라 OST 패키지'
  and p.price = 42000.00
  and p.status = 'ON_SALE'
  and p.content_title = '라스트 오케스트라'
  and p.content_type = 'MUSIC'
  and p.category = 'OST'
  and p.description = 'OST 앨범, 미니 포토북, 넘버링 카드로 구성된 한정판 음악 패키지'
  and not exists (select 1 from product_discounts where product_id = p.id);

insert into coupons (
    name, type, discount_amount, target_product_id, expires_at, created_at, updated_at
)
select
    '드롭 기념 전체 상품 3000원 할인',
    'ORDER',
    3000.00,
    null,
    timestamp '2027-12-31 23:59:59',
    timestamp '2026-06-06 00:00:00',
    timestamp '2026-06-06 00:00:00'
where not exists (select 1 from coupons where name = '드롭 기념 전체 상품 3000원 할인');

insert into coupons (
    name, type, discount_amount, target_product_id, expires_at, created_at, updated_at
)
select
    '드롭 기념 전체 상품 5000원 할인',
    'ORDER',
    5000.00,
    null,
    timestamp '2027-12-31 23:59:59',
    timestamp '2026-06-06 00:00:00',
    timestamp '2026-06-06 00:00:00'
where not exists (select 1 from coupons where name = '드롭 기념 전체 상품 5000원 할인');

insert into coupons (
    name, type, discount_amount, target_product_id, expires_at, created_at, updated_at
)
select
    '달빛 상점 아트북 4000원 할인',
    'PRODUCT',
    4000.00,
    p.id,
    timestamp '2027-12-31 23:59:59',
    timestamp '2026-06-06 00:00:00',
    timestamp '2026-06-06 00:00:00'
from products p
where p.id = 10001
  and p.name = '달빛 상점 한정판 아트북'
  and p.price = 35000.00
  and p.status = 'ON_SALE'
  and p.content_title = '달빛 상점'
  and p.content_type = 'WEBTOON'
  and p.category = 'ARTBOOK'
  and p.description = '웹툰 달빛 상점 시즌 1의 콘셉트 아트와 작가 코멘터리를 담은 한정판 아트북'
  and not exists (select 1 from coupons where name = '달빛 상점 아트북 4000원 할인');

insert into coupons (
    name, type, discount_amount, target_product_id, expires_at, created_at, updated_at
)
select
    '라스트 오케스트라 OST 7000원 할인',
    'PRODUCT',
    7000.00,
    p.id,
    timestamp '2027-12-31 23:59:59',
    timestamp '2026-06-06 00:00:00',
    timestamp '2026-06-06 00:00:00'
from products p
where p.id = 10004
  and p.name = '라스트 오케스트라 OST 패키지'
  and p.price = 42000.00
  and p.status = 'ON_SALE'
  and p.content_title = '라스트 오케스트라'
  and p.content_type = 'MUSIC'
  and p.category = 'OST'
  and p.description = 'OST 앨범, 미니 포토북, 넘버링 카드로 구성된 한정판 음악 패키지'
  and not exists (select 1 from coupons where name = '라스트 오케스트라 OST 7000원 할인');
