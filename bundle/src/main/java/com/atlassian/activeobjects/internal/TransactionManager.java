package com.atlassian.activeobjects.internal;

import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * Handling of transactions. The actions done in the {@link com.atlassian.sal.api.transaction.TransactionCallback}
 * shall be wrapped in a transaction.
 */
public interface TransactionManager
{
    <T> T doInTransaction(TransactionCallback<T> callback);
}
