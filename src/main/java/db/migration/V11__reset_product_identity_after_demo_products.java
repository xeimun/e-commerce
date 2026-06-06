package db.migration;

import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V11__reset_product_identity_after_demo_products extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        long nextProductId = findMaxProductId(context) + 1;

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("alter table products alter column id restart with " + nextProductId);
        }
    }

    private long findMaxProductId(Context context) throws Exception {
        try (
                Statement statement = context.getConnection().createStatement();
                ResultSet resultSet = statement.executeQuery("select coalesce(max(id), 0) from products")
        ) {
            resultSet.next();

            return resultSet.getLong(1);
        }
    }
}
