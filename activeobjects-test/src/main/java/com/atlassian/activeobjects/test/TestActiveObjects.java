package com.atlassian.activeobjects.test;

import com.atlassian.activeobjects.internal.EntityManagedActiveObjects;
import com.atlassian.activeobjects.internal.TransactionManager;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.ActiveObjectsException;
import net.java.ao.EntityManager;
import net.java.ao.Transaction;

import java.sql.SQLException;

public final class TestActiveObjects extends EntityManagedActiveObjects {
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
        }, DatabaseType.HSQL);
    }
}
