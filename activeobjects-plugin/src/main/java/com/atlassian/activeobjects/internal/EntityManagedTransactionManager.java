package com.atlassian.activeobjects.internal;

import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.ActiveObjectsException;
import net.java.ao.EntityManager;
import net.java.ao.Transaction;

import java.sql.SQLException;

import static com.google.common.base.Preconditions.*;

/**
 * Implementation of the {@link com.atlassian.activeobjects.internal.TransactionManager} that
 * relies on Active Objects transaction mechanism.
 */
final class EntityManagedTransactionManager extends AbstractLoggingTransactionManager
{
    private final EntityManager entityManager;

    EntityManagedTransactionManager(EntityManager entityManager)
    {
        this.entityManager = checkNotNull(entityManager);
    }

    <T> T inTransaction(final TransactionCallback<T> callback)
    {
        try
        {
            return new Transaction<T>(entityManager)
            {
                public T run()
                {
                    return callback.doInTransaction();
                }
            }.execute();
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsException(e);
        }
    }
}
