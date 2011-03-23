package com.atlassian.activeobjects.internal;

import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;

import static com.google.common.base.Preconditions.*;

/**
 * Implementation of the {@link com.atlassian.activeobjects.internal.TransactionManager}
 * that relies on SAL's {@link com.atlassian.sal.api.transaction.TransactionTemplate}.
 */
final class SalTransactionManager extends AbstractLoggingTransactionManager
{
    private final TransactionTemplate transactionTemplate;

    SalTransactionManager(TransactionTemplate transactionTemplate)
    {
        this.transactionTemplate = checkNotNull(transactionTemplate);
    }

    <T> T inTransaction(TransactionCallback<T> callback)
    {
        return transactionTemplate.execute(callback);
    }
}
