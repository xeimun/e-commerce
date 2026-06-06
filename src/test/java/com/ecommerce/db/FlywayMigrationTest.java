package com.ecommerce.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class FlywayMigrationTest {

    @Test
    void v5BackfillsSourceCartItemIdForExistingPaymentPendingOrderItems() {
        DriverManagerDataSource dataSource = dataSource();
        migrateToV4(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        insertLegacyPaymentPendingOrder(
                jdbcTemplate,
                "timestamp '2026-06-04 10:00:00'",
                "timestamp '2026-06-04 10:05:00'"
        );

        migrateToLatest(dataSource);

        Long sourceCartItemId = jdbcTemplate.queryForObject(
                "select source_cart_item_id from order_items where id = 600",
                Long.class
        );
        assertThat(sourceCartItemId).isEqualTo(400L);
    }

    @Test
    void v5DoesNotBackfillCartItemCreatedAfterExistingPaymentPendingOrder() {
        DriverManagerDataSource dataSource = dataSource();
        migrateToV4(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        insertLegacyPaymentPendingOrder(
                jdbcTemplate,
                "timestamp '2026-06-04 10:10:00'",
                "timestamp '2026-06-04 10:05:00'"
        );

        migrateToLatest(dataSource);

        Long sourceCartItemId = jdbcTemplate.queryForObject(
                "select source_cart_item_id from order_items where id = 600",
                Long.class
        );
        assertThat(sourceCartItemId).isNull();
    }

    @Test
    void v11RemovesUnreferencedDemoChildRowsAttachedToCollidingProductId() {
        DriverManagerDataSource dataSource = dataSource();
        migrateToV8(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        insertCollidingProduct(jdbcTemplate);
        migrateToV10(dataSource);
        insertInvalidDemoProductCoupon(jdbcTemplate, 800L);

        migrateToLatest(dataSource);

        String productName = jdbcTemplate.queryForObject(
                "select name from products where id = 10001",
                String.class
        );
        Integer stockCount = jdbcTemplate.queryForObject(
                "select count(*) from stocks where product_id = 10001",
                Integer.class
        );
        Integer discountCount = jdbcTemplate.queryForObject(
                "select count(*) from product_discounts where product_id = 10001",
                Integer.class
        );
        Integer productCouponCount = jdbcTemplate.queryForObject(
                "select count(*) from coupons where target_product_id = 10001",
                Integer.class
        );
        assertThat(productName).isEqualTo("기존 상품");
        assertThat(stockCount).isZero();
        assertThat(discountCount).isZero();
        assertThat(productCouponCount).isZero();
    }

    @Test
    void v11ExpiresReferencedInvalidDemoProductCouponInsteadOfDeletingIt() {
        DriverManagerDataSource dataSource = dataSource();
        migrateToV8(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        insertCollidingProduct(jdbcTemplate);
        migrateToV10(dataSource);
        Long couponId = 801L;
        insertInvalidDemoProductCoupon(jdbcTemplate, couponId);
        insertIssuedCoupon(jdbcTemplate, 700L, couponId, 11L);

        migrateToLatest(dataSource);

        Integer couponCount = jdbcTemplate.queryForObject(
                "select count(*) from coupons where id = ? and expires_at = timestamp '2026-06-06 00:00:00'",
                Integer.class,
                couponId
        );
        String issuedCouponStatus = jdbcTemplate.queryForObject(
                "select status from issued_coupons where id = 700",
                String.class
        );
        assertThat(couponCount).isEqualTo(1);
        assertThat(issuedCouponStatus).isEqualTo("EXPIRED");
    }

    @Test
    void v10ResetsProductIdentityToCurrentMaxIdAfterExplicitDemoProductIds() {
        DriverManagerDataSource dataSource = dataSource();
        migrateToV8(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        insertExistingProduct(jdbcTemplate, 20000L, "기존 고ID 상품");

        migrateToLatest(dataSource);

        jdbcTemplate.update("""
                insert into products (
                    name, price, status, content_title, content_type, category, description, created_at, updated_at
                )
                values (
                    '마이그레이션 이후 등록 상품', 12000.00, 'ON_SALE',
                    '테스트 콘텐츠', 'ETC', 'TEST', '마이그레이션 이후 자동 ID 검증 상품',
                    timestamp '2026-06-07 00:00:00', timestamp '2026-06-07 00:00:00'
                )
                """);

        Long productId = jdbcTemplate.queryForObject(
                "select id from products where name = '마이그레이션 이후 등록 상품'",
                Long.class
        );
        assertThat(productId).isGreaterThan(20000L);
    }

    @Test
    void v14RemovesDropDemo3000OrderCouponFromFinalSeed() {
        DriverManagerDataSource dataSource = dataSource();
        migrateToLatest(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Integer oldNameCount = jdbcTemplate.queryForObject(
                "select count(*) from coupons where name = '첫 주문 전체 상품 3000원 할인'",
                Integer.class
        );
        Integer newNameCount = jdbcTemplate.queryForObject(
                "select count(*) from coupons where name = '드롭 기념 전체 상품 3000원 할인'",
                Integer.class
        );
        assertThat(oldNameCount).isZero();
        assertThat(newNameCount).isZero();
    }

    @Test
    void v11DoesNotRenameNonSeedOrderCouponWithSameNameAndAmount() {
        DriverManagerDataSource dataSource = dataSource();
        migrateToV10(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        insertNonSeedOrderCouponWithOldDemoName(jdbcTemplate, 900L);

        migrateToLatest(dataSource);

        String couponName = jdbcTemplate.queryForObject(
                "select name from coupons where id = 900",
                String.class
        );
        Integer renamedDemoCouponCount = jdbcTemplate.queryForObject(
                "select count(*) from coupons where name = '드롭 기념 전체 상품 3000원 할인'",
                Integer.class
        );
        assertThat(couponName).isEqualTo("첫 주문 전체 상품 3000원 할인");
        assertThat(renamedDemoCouponCount).isZero();
    }

    @Test
    void v13UpdatesFinalCatalogPricesAndCategories() {
        DriverManagerDataSource dataSource = dataSource();
        migrateToLatest(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        var productNames = jdbcTemplate.queryForList(
                "select name from products where id between 10001 and 10005 order by id",
                String.class
        );
        var productCategories = jdbcTemplate.queryForList(
                "select category from products where id between 10001 and 10005 order by id",
                String.class
        );
        var productPrices = jdbcTemplate.queryForList(
                "select price from products where id between 10001 and 10005 order by id",
                BigDecimal.class
        );
        Integer activeLpDiscountCount = jdbcTemplate.queryForObject(
                "select count(*) from product_discounts where product_id = 10001 and active = true",
                Integer.class
        );
        var productCouponNames = jdbcTemplate.queryForList(
                "select name from coupons where type = 'PRODUCT' and target_product_id in (10001, 10004) order by target_product_id",
                String.class
        );

        assertThat(productNames).containsExactly(
                "[Sunny Side Up] Original Soundtrack Limited LP",
                "[Savoia] S-21 수상비행기 피규어",
                "[구름을 만드는 정비소] 하레 작가 드로잉 원화",
                "[우주에서 온 묘코] 묘코 마스코트 봉제 인형",
                "[사이버펑크 2088] 네온 로고 아크릴 키링"
        );
        assertThat(productCategories).containsExactly(
                "음반",
                "피규어",
                "아트워크",
                "봉제 인형",
                "키링"
        );
        assertThat(productPrices).containsExactly(
                new BigDecimal("179000.00"),
                new BigDecimal("88000.00"),
                new BigDecimal("60000.00"),
                new BigDecimal("45000.00"),
                new BigDecimal("12000.00")
        );
        assertThat(activeLpDiscountCount).isZero();
        assertThat(productCouponNames).containsExactly(
                "Sunny Side Up LP 4000원 할인",
                "묘코 마스코트 봉제 인형 7000원 할인"
        );
    }

    @Test
    void v14AppliesFinalDemoInstantDiscountsAndKeepsOnly5000OrderCoupon() {
        DriverManagerDataSource dataSource = dataSource();
        migrateToLatest(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        var activeDiscountNames = jdbcTemplate.queryForList(
                "select name from product_discounts where product_id in (10002, 10004) and active = true order by product_id",
                String.class
        );
        var activeDiscountAmounts = jdbcTemplate.queryForList(
                "select discount_amount from product_discounts where product_id in (10002, 10004) and active = true order by product_id",
                BigDecimal.class
        );
        Integer removed3000OrderCouponCount = jdbcTemplate.queryForObject(
                "select count(*) from coupons where name = '드롭 기념 전체 상품 3000원 할인' and type = 'ORDER'",
                Integer.class
        );
        Integer remaining5000OrderCouponCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from coupons
                        where name = '드롭 기념 전체 상품 5000원 할인'
                          and type = 'ORDER'
                          and discount_amount = 5000.00
                          and target_product_id is null
                        """,
                Integer.class
        );

        assertThat(activeDiscountNames).containsExactly(
                "Savoia S-21 드롭 할인",
                "묘코 봉제 인형 드롭 할인"
        );
        assertThat(activeDiscountAmounts).containsExactly(
                new BigDecimal("5000.00"),
                new BigDecimal("3000.00")
        );
        assertThat(removed3000OrderCouponCount).isZero();
        assertThat(remaining5000OrderCouponCount).isEqualTo(1);
    }

    @Test
    void v14KeepsReserved3000OrderCouponReservedForPendingOrderLifecycle() {
        DriverManagerDataSource dataSource = dataSource();
        migrateToV13(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Long couponId = jdbcTemplate.queryForObject(
                """
                        select id
                        from coupons
                        where name = '드롭 기념 전체 상품 3000원 할인'
                          and type = 'ORDER'
                          and discount_amount = 3000.00
                          and target_product_id is null
                        """,
                Long.class
        );
        insertIssuedCoupon(jdbcTemplate, 710L, couponId, 31L);
        insertPendingOrder(jdbcTemplate, 910L, 32L);
        insertReservedIssuedCoupon(jdbcTemplate, 711L, couponId, 32L, 910L);

        migrateToLatest(dataSource);

        var statuses = jdbcTemplate.queryForList(
                "select status from issued_coupons where id in (710, 711) order by id",
                String.class
        );
        Long reservedOrderId = jdbcTemplate.queryForObject(
                "select order_id from issued_coupons where id = 711",
                Long.class
        );

        assertThat(statuses).containsExactly("EXPIRED", "RESERVED");
        assertThat(reservedOrderId).isEqualTo(910L);
    }

    @Test
    void v14DoesNotCleanupNonSeed3000OrderCouponsWithSameDisplayValues() {
        DriverManagerDataSource dataSource = dataSource();
        migrateToV13(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        insertNonSeedOrderCouponWithDropDemoName(jdbcTemplate, 901L);
        insertNonSeedOrderCouponWithDropDemoName(jdbcTemplate, 902L);
        insertIssuedCoupon(jdbcTemplate, 720L, 902L, 41L);

        migrateToLatest(dataSource);

        Integer nonSeedCouponCount = jdbcTemplate.queryForObject(
                "select count(*) from coupons where id in (901, 902)",
                Integer.class
        );
        String issuedCouponStatus = jdbcTemplate.queryForObject(
                "select status from issued_coupons where id = 720",
                String.class
        );
        var nonSeedExpiresAt = jdbcTemplate.queryForList(
                "select expires_at from coupons where id in (901, 902) order by id",
                java.sql.Timestamp.class
        );

        assertThat(nonSeedCouponCount).isEqualTo(2);
        assertThat(issuedCouponStatus).isEqualTo("AVAILABLE");
        assertThat(nonSeedExpiresAt)
                .extracting(java.sql.Timestamp::toLocalDateTime)
                .containsExactly(
                        java.time.LocalDateTime.of(2028, 12, 31, 23, 59, 59),
                        java.time.LocalDateTime.of(2028, 12, 31, 23, 59, 59)
                );
    }

    private DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        return dataSource;
    }

    private void migrateToV4(DriverManagerDataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("4"))
                .load()
                .migrate();
    }

    private void migrateToV8(DriverManagerDataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("8"))
                .load()
                .migrate();
    }

    private void migrateToV10(DriverManagerDataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("10"))
                .load()
                .migrate();
    }

    private void migrateToV13(DriverManagerDataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("13"))
                .load()
                .migrate();
    }

    private void migrateToLatest(DriverManagerDataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private void insertLegacyPaymentPendingOrder(
            JdbcTemplate jdbcTemplate,
            String cartItemCreatedAt,
            String orderCreatedAt
    ) {
        jdbcTemplate.update("""
                insert into products (
                    id, name, price, status, content_title, content_type, category, description, created_at, updated_at
                )
                values (
                    100, '달빛 상점 한정판 아트북', 35000.00, 'ON_SALE',
                    '달빛 상점', 'WEBTOON', 'ARTBOOK', '웹툰 달빛 상점의 시즌 1 한정판 아트북',
                    timestamp '2026-06-04 09:50:00', timestamp '2026-06-04 09:50:00'
                )
                """);
        jdbcTemplate.update("""
                insert into stocks (id, product_id, quantity, created_at, updated_at)
                values (200, 100, 3, timestamp '2026-06-04 09:50:00', timestamp '2026-06-04 09:50:00')
                """);
        jdbcTemplate.update("""
                insert into carts (id, customer_id, created_at, updated_at)
                values (300, 7, timestamp '2026-06-04 09:55:00', timestamp '2026-06-04 09:55:00')
                """);
        jdbcTemplate.update("""
                insert into cart_items (id, cart_id, product_id, quantity, created_at, updated_at)
                values (400, 300, 100, 2, %s, %s)
                """.formatted(cartItemCreatedAt, cartItemCreatedAt));
        jdbcTemplate.update("""
                insert into orders (
                    id, customer_id, status, expires_at, cancel_reason,
                    total_product_amount, total_instant_discount_amount, total_coupon_discount_amount,
                    final_payment_amount, created_at, updated_at, paid_at, payment_canceled_at
                )
                values (
                    500, 7, 'PAYMENT_PENDING', timestamp '2026-06-04 10:15:00', null,
                    70000.00, 0.00, 0.00, 70000.00,
                    %s, %s, null, null
                )
                """.formatted(orderCreatedAt, orderCreatedAt));
        jdbcTemplate.update("""
                insert into order_items (
                    id, order_id, product_id, product_name, product_price, quantity,
                    original_amount, instant_discount_amount, product_coupon_discount_amount, final_amount,
                    created_at, updated_at
                )
                values (
                    600, 500, 100, '달빛 상점 한정판 아트북', 35000.00, 2,
                    70000.00, 0.00, 0.00, 70000.00,
                    %s, %s
                )
                """.formatted(orderCreatedAt, orderCreatedAt));
    }

    private void insertCollidingProduct(JdbcTemplate jdbcTemplate) {
        insertExistingProduct(jdbcTemplate, 10001L, "기존 상품");
    }

    private void insertIssuedCoupon(
            JdbcTemplate jdbcTemplate,
            Long issuedCouponId,
            Long couponId,
            Long customerId
    ) {
        jdbcTemplate.update("""
                insert into issued_coupons (
                    id, coupon_id, customer_id, status, issued_at, created_at, updated_at
                )
                values (
                    ?, ?, ?, 'AVAILABLE',
                    timestamp '2026-06-06 12:00:00',
                    timestamp '2026-06-06 12:00:00',
                    timestamp '2026-06-06 12:00:00'
                )
                """, issuedCouponId, couponId, customerId);
    }

    private void insertReservedIssuedCoupon(
            JdbcTemplate jdbcTemplate,
            Long issuedCouponId,
            Long couponId,
            Long customerId,
            Long orderId
    ) {
        jdbcTemplate.update("""
                insert into issued_coupons (
                    id, coupon_id, customer_id, order_id, status, issued_at, reserved_at, created_at, updated_at
                )
                values (
                    ?, ?, ?, ?, 'RESERVED',
                    timestamp '2026-06-07 12:00:00',
                    timestamp '2026-06-07 12:01:00',
                    timestamp '2026-06-07 12:00:00',
                    timestamp '2026-06-07 12:01:00'
                )
                """, issuedCouponId, couponId, customerId, orderId);
    }

    private void insertPendingOrder(JdbcTemplate jdbcTemplate, Long orderId, Long customerId) {
        jdbcTemplate.update("""
                insert into orders (
                    id, customer_id, status, expires_at, cancel_reason,
                    total_product_amount, total_instant_discount_amount, total_coupon_discount_amount,
                    final_payment_amount, created_at, updated_at, paid_at, payment_canceled_at
                )
                values (
                    ?, ?, 'PAYMENT_PENDING', timestamp '2026-06-07 12:11:00', null,
                    88000.00, 0.00, 3000.00, 85000.00,
                    timestamp '2026-06-07 12:01:00', timestamp '2026-06-07 12:01:00',
                    null, null
                )
                """, orderId, customerId);
    }

    private void insertInvalidDemoProductCoupon(JdbcTemplate jdbcTemplate, Long couponId) {
        jdbcTemplate.update("""
                insert into coupons (
                    id, name, type, discount_amount, target_product_id,
                    expires_at, created_at, updated_at
                )
                values (
                    ?, '달빛 상점 아트북 4000원 할인', 'PRODUCT', 4000.00, 10001,
                    timestamp '2027-12-31 23:59:59',
                    timestamp '2026-06-06 00:00:00',
                    timestamp '2026-06-06 00:00:00'
                )
                """, couponId);
    }

    private void insertNonSeedOrderCouponWithOldDemoName(JdbcTemplate jdbcTemplate, Long couponId) {
        jdbcTemplate.update("""
                insert into coupons (
                    id, name, type, discount_amount, target_product_id,
                    expires_at, created_at, updated_at
                )
                values (
                    ?, '첫 주문 전체 상품 3000원 할인', 'ORDER', 3000.00, null,
                    timestamp '2028-12-31 23:59:59',
                    timestamp '2026-06-07 00:00:00',
                    timestamp '2026-06-07 00:00:00'
                )
                """, couponId);
    }

    private void insertNonSeedOrderCouponWithDropDemoName(JdbcTemplate jdbcTemplate, Long couponId) {
        jdbcTemplate.update("""
                insert into coupons (
                    id, name, type, discount_amount, target_product_id,
                    expires_at, created_at, updated_at
                )
                values (
                    ?, '드롭 기념 전체 상품 3000원 할인', 'ORDER', 3000.00, null,
                    timestamp '2028-12-31 23:59:59',
                    timestamp '2026-06-07 12:00:00',
                    timestamp '2026-06-07 12:00:00'
                )
                """, couponId);
    }

    private void insertExistingProduct(JdbcTemplate jdbcTemplate, Long productId, String name) {
        jdbcTemplate.update("""
                insert into products (
                    id, name, price, status, content_title, content_type, category, description, created_at, updated_at
                )
                values (
                    ?, ?, 10000.00, 'ON_SALE',
                    '기존 콘텐츠', 'ETC', 'LEGACY', '데모 상품 ID와 충돌하는 기존 상품',
                    timestamp '2026-06-05 00:00:00', timestamp '2026-06-05 00:00:00'
                )
                """, productId, name);
    }
}
