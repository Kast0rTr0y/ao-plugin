package com.atlassian.activeobjects.test;

import com.atlassian.activeobjects.internal.EntityManagedActiveObjects;
import com.atlassian.activeobjects.internal.TransactionManager;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.ImmutableMap;
import net.java.ao.ActiveObjectsException;
import net.java.ao.EntityManager;
import net.java.ao.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public final class TestActiveObjects extends EntityManagedActiveObjects {

    private static final Map<String, DatabaseType> DATABASE_PRODUCT_TO_TYPE_MAP = ImmutableMap.<String, DatabaseType>builder()
            .put("HSQL Database Engine", DatabaseType.HSQL)
            .put("MySQL", DatabaseType.MYSQL)
            .put("PostgreSQL", DatabaseType.POSTGRESQL)
            .put("Oracle", DatabaseType.ORACLE)
            .put("Microsoft SQL Server", DatabaseType.MS_SQL)
            .put("DB2", DatabaseType.DB2)
            .build();

    public TestActiveObjects(final EntityManager entityManager) {
        super(entityManager, new TransactionManager() {
            public <T> T doInTransaction(final TransactionCallback<T> callback) {
                try {
                    return new Transaction<T>(entityManager) {
                        public T run() {
                            return callback.doInTransaction();
                        }
                    }.execute();
                } catch (SQLException e) {
                    throw new ActiveObjectsException(e);
                }
            }
        }, findDatabaseType(entityManager));
    }

    private static DatabaseType findDatabaseType(EntityManager entityManager) {
        Connection connection = null;
        try {
            connection = entityManager.getProvider().getConnection();
            String dbName = connection.getMetaData().getDatabaseProductName();
            for (Map.Entry<String, DatabaseType> entry : DATABASE_PRODUCT_TO_TYPE_MAP.entrySet()) {
                // We use "startsWith" here, because the ProductName for DB2 contains OS information.
                if (dbName.startsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
        } catch (SQLException e) {
            throw new ActiveObjectsException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new ActiveObjectsException(e);
                }
            }
        }

        return DatabaseType.UNKNOWN;
    }
}