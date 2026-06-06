package com.ecommerce.db;

import static org.assertj.core.api.Assertions.assertThat;

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
        Long couponId = jdbcTemplate.queryForObject(
                "select id from coupons where name = '달빛 상점 아트북 4000원 할인' and target_product_id = 10001",
                Long.class
        );
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
    void v11RenamesFirstOrderDemoCouponToDropDemoCoupon() {
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
        assertThat(newNameCount).isEqualTo(1);
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
