package com.atlassian.activeobjects.internal;

import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import net.java.ao.EntityManager;

import static com.google.common.base.Preconditions.*;

/**
 * Implementation of the {@link com.atlassian.activeobjects.internal.TransactionManager}
 * that relies on SAL's {@link com.atlassian.sal.api.transaction.TransactionTemplate}.
 */
final class SalTransactionManager extends AbstractLoggingTransactionManager
{
    private final TransactionTemplate transactionTemplate;

    private final EntityManager entityManager;

    SalTransactionManager(TransactionTemplate transactionTemplate, EntityManager entityManager)
    {
        this.transactionTemplate = checkNotNull(transactionTemplate);
        this.entityManager = checkNotNull(entityManager);
    }

    <T> T inTransaction(TransactionCallback<T> callback)
    {
        try
        {
            return transactionTemplate.execute(callback);
        }
        catch (final RuntimeException exception)
        {
            entityManager.flushDirty();
            throw exception;
        }
    }
}
