update products
set price = 179000.00,
    category = '음반',
    updated_at = timestamp '2026-06-07 00:00:00'
where id = 10001
  and name = '[Sunny Side Up] Original Soundtrack Limited LP';

update products
set price = 88000.00,
    category = '피규어',
    updated_at = timestamp '2026-06-07 00:00:00'
where id = 10002
  and name = '[Savoia] S-21 수상비행기 피규어';

update products
set price = 60000.00,
    category = '아트워크',
    updated_at = timestamp '2026-06-07 00:00:00'
where id = 10003
  and name = '[구름을 만드는 정비소] 하레 작가 드로잉 원화';

update products
set price = 45000.00,
    category = '봉제 인형',
    updated_at = timestamp '2026-06-07 00:00:00'
where id = 10004
  and name = '[우주에서 온 묘코] 묘코 마스코트 봉제 인형';

update products
set price = 12000.00,
    category = '키링',
    updated_at = timestamp '2026-06-07 00:00:00'
where id = 10005
  and name = '[사이버펑크 2088] 네온 로고 아크릴 키링';

update product_discounts
set active = false,
    updated_at = timestamp '2026-06-07 00:00:00'
where product_id in (10001, 10004)
  and name in ('Sunny Side Up LP 발매 할인', '묘코 마스코트 출시 할인')
  and active = true;
