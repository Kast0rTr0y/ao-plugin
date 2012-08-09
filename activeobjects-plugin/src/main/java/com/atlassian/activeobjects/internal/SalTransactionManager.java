package com.atlassian.activeobjects.internal;

import static com.google.common.base.Preconditions.checkNotNull;
import net.java.ao.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.spi.TransactionSynchronisationManager;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;

/**
 * Implementation of the {@link com.atlassian.activeobjects.internal.TransactionManager}
 * that relies on SAL's {@link com.atlassian.sal.api.transaction.TransactionTemplate}.
 */
final class SalTransactionManager extends AbstractLoggingTransactionManager
{
    private final TransactionTemplate transactionTemplate;

    private final EntityManager entityManager;
    
    private final TransactionSynchronisationManager synchManager;

    private final Logger log = LoggerFactory.getLogger(SalTransactionManager.class);

    SalTransactionManager(TransactionTemplate transactionTemplate,
            EntityManager entityManager,
            TransactionSynchronisationManager synchManager)
    {
        this.transactionTemplate = checkNotNull(transactionTemplate);
        this.entityManager = checkNotNull(entityManager);
        this.synchManager = checkNotNull(synchManager);
    }

    <T> T inTransaction(TransactionCallback<T> callback)
    {
        final Runnable commitAction = createCommitAction(entityManager);
        final Runnable rollBackAction = createRollbackAction(entityManager);
        final boolean transactionSynced = synchManager.runOnSuccessfulCommit(commitAction);
        if (transactionSynced)
        {
            synchManager.runOnRollBack(rollBackAction);
        }
        final T result;
        try
        {
            result = transactionTemplate.execute(callback);
        }
        catch (final RuntimeException exception)
        {
            if(!transactionSynced)
            {
                try
                {
                    rollBackAction.run();
                }
                catch(Exception ex)
                {
                    log.error("Error occurred performing post roll back action, logging and throwing original exception", ex);
                }
            }
            throw exception;
        }
        if (!transactionSynced) {
            commitAction.run();
        }
        return result;
    }

    private Runnable createCommitAction(final EntityManager entityManager)
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                log.debug("Flushing entityManager due to commit");
                entityManager.flushEntityCache();
            }
        };
    }

    private Runnable createRollbackAction(final EntityManager entityManager)
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                log.info("Flushing entityManager due to rollback");
                entityManager.flushAll();
            }
        };
    }
}
