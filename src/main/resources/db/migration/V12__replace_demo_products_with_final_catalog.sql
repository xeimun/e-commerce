update products
set name = '[Sunny Side Up] Original Soundtrack Limited LP',
    content_title = 'Sunny Side Up',
    content_type = 'MUSIC',
    category = '한정 LP',
    description = 'Sunny Side Up 오리지널 사운드트랙을 바이닐로 담은 한정판 LP',
    updated_at = timestamp '2026-06-07 00:00:00'
where id = 10001
  and name = '달빛 상점 한정판 아트북'
  and price = 35000.00
  and status = 'ON_SALE'
  and content_title = '달빛 상점'
  and content_type = 'WEBTOON'
  and category = 'ARTBOOK'
  and description = '웹툰 달빛 상점 시즌 1의 콘셉트 아트와 작가 코멘터리를 담은 한정판 아트북';

update products
set name = '[Savoia] S-21 수상비행기 피규어',
    content_title = 'Savoia',
    content_type = 'MOVIE',
    category = '수상비행기 피규어',
    description = 'Savoia 세계관의 S-21 수상비행기를 재현한 전시용 피규어',
    updated_at = timestamp '2026-06-07 00:00:00'
where id = 10002
  and name = '검은별 기록관 포스터 세트'
  and price = 18000.00
  and status = 'ON_SALE'
  and content_title = '검은별 기록관'
  and content_type = 'WEB_NOVEL'
  and category = 'POSTER'
  and description = '다크 판타지 웹소설 검은별 기록관의 주요 장면을 담은 A3 포스터 3종 세트';

update products
set name = '[구름을 만드는 정비소] 하레 작가 드로잉 원화',
    content_title = '구름을 만드는 정비소',
    content_type = 'WEBTOON',
    category = '드로잉 원화',
    description = '하레 작가의 구름 공방 콘셉트가 담긴 드로잉 원화',
    updated_at = timestamp '2026-06-07 00:00:00'
where id = 10003
  and name = '푸른 사서의 방 아크릴 스탠드'
  and price = 22000.00
  and status = 'ON_SALE'
  and content_title = '푸른 사서의 방'
  and content_type = 'DRAMA'
  and category = 'ACRYLIC_STAND'
  and description = '드라마 푸른 사서의 방 주인공 일러스트를 활용한 데스크 아크릴 스탠드';

update products
set name = '[우주에서 온 묘코] 묘코 마스코트 봉제 인형',
    content_title = '우주에서 온 묘코',
    content_type = 'WEB_NOVEL',
    category = '마스코트 봉제 인형',
    description = '우주에서 온 묘코의 별빛 디테일을 살린 마스코트 봉제 인형',
    updated_at = timestamp '2026-06-07 00:00:00'
where id = 10004
  and name = '라스트 오케스트라 OST 패키지'
  and price = 42000.00
  and status = 'ON_SALE'
  and content_title = '라스트 오케스트라'
  and content_type = 'MUSIC'
  and category = 'OST'
  and description = 'OST 앨범, 미니 포토북, 넘버링 카드로 구성된 한정판 음악 패키지';

update products
set name = '[사이버펑크 2088] 네온 로고 아크릴 키링',
    content_title = '사이버펑크 2088',
    content_type = 'DRAMA',
    category = '아크릴 키링',
    description = '사이버펑크 2088의 네온 로고를 투명 아크릴로 제작한 키링',
    updated_at = timestamp '2026-06-07 00:00:00'
where id = 10005
  and name = '새벽의 문장 스티커팩'
  and price = 9000.00
  and status = 'ON_SALE'
  and content_title = '새벽의 문장'
  and content_type = 'MOVIE'
  and category = 'STICKER'
  and description = '영화 새벽의 문장 속 상징 문구와 오브젝트를 담은 다이어리 스티커팩';

update product_discounts
set name = 'Sunny Side Up LP 발매 할인',
    updated_at = timestamp '2026-06-07 00:00:00'
where product_id = 10001
  and name = '드롭 오픈 할인'
  and discount_amount = 3000.00
  and starts_at = timestamp '2026-06-01 00:00:00'
  and ends_at = timestamp '2027-12-31 23:59:59'
  and active = true
  and created_at = timestamp '2026-06-06 00:00:00'
  and exists (
      select 1
      from products
      where id = 10001
        and name = '[Sunny Side Up] Original Soundtrack Limited LP'
  );

update product_discounts
set name = '묘코 마스코트 출시 할인',
    updated_at = timestamp '2026-06-07 00:00:00'
where product_id = 10004
  and name = 'OST 예약 할인'
  and discount_amount = 5000.00
  and starts_at = timestamp '2026-06-01 00:00:00'
  and ends_at = timestamp '2027-12-31 23:59:59'
  and active = true
  and created_at = timestamp '2026-06-06 00:00:00'
  and exists (
      select 1
      from products
      where id = 10004
        and name = '[우주에서 온 묘코] 묘코 마스코트 봉제 인형'
  );

update coupons
set name = 'Sunny Side Up LP 4000원 할인',
    updated_at = timestamp '2026-06-07 00:00:00'
where name = '달빛 상점 아트북 4000원 할인'
  and type = 'PRODUCT'
  and discount_amount = 4000.00
  and target_product_id = 10001
  and expires_at = timestamp '2027-12-31 23:59:59'
  and created_at = timestamp '2026-06-06 00:00:00'
  and exists (
      select 1
      from products
      where id = 10001
        and name = '[Sunny Side Up] Original Soundtrack Limited LP'
  );

update coupons
set name = '묘코 마스코트 봉제 인형 7000원 할인',
    updated_at = timestamp '2026-06-07 00:00:00'
where name = '라스트 오케스트라 OST 7000원 할인'
  and type = 'PRODUCT'
  and discount_amount = 7000.00
  and target_product_id = 10004
  and expires_at = timestamp '2027-12-31 23:59:59'
  and created_at = timestamp '2026-06-06 00:00:00'
  and exists (
      select 1
      from products
      where id = 10004
        and name = '[우주에서 온 묘코] 묘코 마스코트 봉제 인형'
  );
