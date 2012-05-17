package com.atlassian.activeobjects.internal;

import com.atlassian.sal.api.transaction.TransactionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract implementation that log at debug level runtime exception that cross the boundary
 * of a transaction.
 */
abstract class AbstractLoggingTransactionManager implements TransactionManager
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public final <T> T doInTransaction(TransactionCallback<T> callback)
    {
        try
        {
            return inTransaction(callback);
        }
        catch (RuntimeException e)
        {
            logger.debug("Exception thrown within transaction", e);
            throw e;
        }
    }

    abstract <T> T inTransaction(TransactionCallback<T> callback);
}
