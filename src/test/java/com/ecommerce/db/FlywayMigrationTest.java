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
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("4"))
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        insertLegacyPaymentPendingOrder(jdbcTemplate);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        Long sourceCartItemId = jdbcTemplate.queryForObject(
                "select source_cart_item_id from order_items where id = 600",
                Long.class
        );
        assertThat(sourceCartItemId).isEqualTo(400L);
    }

    private void insertLegacyPaymentPendingOrder(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                insert into products (
                    id, name, price, status, content_title, content_type, category, description, created_at, updated_at
                )
                values (
                    100, '달빛 상점 한정판 아트북', 35000.00, 'ON_SALE',
                    '달빛 상점', 'WEBTOON', 'ARTBOOK', '웹툰 달빛 상점의 시즌 1 한정판 아트북',
                    current_timestamp, current_timestamp
                )
                """);
        jdbcTemplate.update("""
                insert into stocks (id, product_id, quantity, created_at, updated_at)
                values (200, 100, 3, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into carts (id, customer_id, created_at, updated_at)
                values (300, 7, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into cart_items (id, cart_id, product_id, quantity, created_at, updated_at)
                values (400, 300, 100, 2, current_timestamp, current_timestamp)
                """);
        jdbcTemplate.update("""
                insert into orders (
                    id, customer_id, status, expires_at, cancel_reason,
                    total_product_amount, total_instant_discount_amount, total_coupon_discount_amount,
                    final_payment_amount, created_at, updated_at, paid_at, payment_canceled_at
                )
                values (
                    500, 7, 'PAYMENT_PENDING', current_timestamp, null,
                    70000.00, 0.00, 0.00, 70000.00,
                    current_timestamp, current_timestamp, null, null
                )
                """);
        jdbcTemplate.update("""
                insert into order_items (
                    id, order_id, product_id, product_name, product_price, quantity,
                    original_amount, instant_discount_amount, product_coupon_discount_amount, final_amount,
                    created_at, updated_at
                )
                values (
                    600, 500, 100, '달빛 상점 한정판 아트북', 35000.00, 2,
                    70000.00, 0.00, 0.00, 70000.00,
                    current_timestamp, current_timestamp
                )
                """);
    }
}
