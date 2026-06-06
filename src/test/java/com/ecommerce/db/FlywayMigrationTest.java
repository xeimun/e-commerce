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
    void v9DoesNotAttachDemoChildRowsToCollidingProductId() {
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
        jdbcTemplate.update("""
                insert into products (
                    id, name, price, status, content_title, content_type, category, description, created_at, updated_at
                )
                values (
                    10001, '기존 상품', 10000.00, 'ON_SALE',
                    '기존 콘텐츠', 'ETC', 'LEGACY', '데모 상품 ID와 충돌하는 기존 상품',
                    timestamp '2026-06-05 00:00:00', timestamp '2026-06-05 00:00:00'
                )
                """);
    }
}
