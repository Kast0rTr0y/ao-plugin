package com.atlassian.activeobjects.confluence;

import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.activeobjects.spi.DefaultInitExecutorServiceProvider;
import com.atlassian.activeobjects.spi.InitExecutorServiceProvider;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;

public class ConfluenceInitExecutorServiceProvider implements InitExecutorServiceProvider
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultInitExecutorServiceProvider.class);

    private final DataSourceProvider dataSourceProvider;
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    @VisibleForTesting
    final InitExecutorServiceProvider defaultInitExecutorServiceProvider;

    public ConfluenceInitExecutorServiceProvider(
            final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory,
            final DataSourceProvider dataSourceProvider,
            final InitExecutorServiceProvider defaultInitExecutorServiceProvider)
    {
        this.threadLocalDelegateExecutorFactory = checkNotNull(threadLocalDelegateExecutorFactory);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.defaultInitExecutorServiceProvider = checkNotNull(defaultInitExecutorServiceProvider);
    }

    /**
     * In the case of HSQL, confluence will dispatch TenantArrivedEvent synchronously. We then execute the init operations
     * in the same thread, so that we don't have multi threads i.e. transactions attempting DDL against the alleged
     * database HSQL.
     *
     * In other cases it will defer to the default.
     *
     * @return same thread executor for HSQL
     */
    @Override
    public ExecutorService initExecutorService(String name)
    {
        DatabaseType databaseType = dataSourceProvider.getDatabaseType();
        if (DatabaseType.HSQL.equals(databaseType))
        {
            logger.debug("creating HSQL snowflake init executor");
            return threadLocalDelegateExecutorFactory.createExecutorService(MoreExecutors.sameThreadExecutor());
        }
        else
        {
            return defaultInitExecutorServiceProvider.initExecutorService(name);
        }
    }
}
