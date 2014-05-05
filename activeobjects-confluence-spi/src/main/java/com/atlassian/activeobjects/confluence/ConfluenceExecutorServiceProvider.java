package com.atlassian.activeobjects.confluence;

import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.activeobjects.spi.ExecutorServiceProvider;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class ConfluenceExecutorServiceProvider implements ExecutorServiceProvider
{
    private final TransactionTemplate transactionTemplate;
    private final DataSourceProvider dataSourceProvider;
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    public ConfluenceExecutorServiceProvider(
            final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory,
            final DataSourceProvider dataSourceProvider,
            final TransactionTemplate transactionTemplate)
    {
        this.threadLocalDelegateExecutorFactory = checkNotNull(threadLocalDelegateExecutorFactory);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.transactionTemplate = checkNotNull(transactionTemplate);
    }

    /**
     * In the case of HSQL, confluence will dispatch TenantArrivedEvent synchronously. We then execute the init operations
     * in the same thread, so that we don't have multi threads i.e. transactions attempting DDL against the alleged
     * database HSQL.
     *
     * @return same thread executor for HSQL
     */
    @Override
    public ExecutorService initExecutorService()
    {
        DatabaseType databaseType = transactionTemplate.execute(new TransactionCallback<DatabaseType>()
        {
            @Override
            public DatabaseType doInTransaction()
            {
                return checkNotNull(dataSourceProvider.getDatabaseType(), dataSourceProvider + " returned null for dbType");
            }
        });

        if (DatabaseType.HSQL.equals(databaseType))
        {
            return threadLocalDelegateExecutorFactory.createExecutorService(MoreExecutors.sameThreadExecutor());
        }
        else
        {
            return null;
        }
    }
}
