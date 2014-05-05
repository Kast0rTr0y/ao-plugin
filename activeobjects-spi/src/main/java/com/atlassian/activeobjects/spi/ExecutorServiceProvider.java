package com.atlassian.activeobjects.spi;

import java.util.concurrent.ExecutorService;

public interface ExecutorServiceProvider
{
    /**
     * Some "special" data sources (e.g. HSQLDB) may require that DDL be executed in a particular thread.
     *
     * In this case, this method should return an {@link java.util.concurrent.ExecutorService} which is appropriate. This
     * thread will be used to execute DDL, then upgrade tasks.
     *
     * The result should have been wrapped in a thread local friendly manner by
     * {@link com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory}.
     *
     * If the data source does not have any such requirements, simply return null and AO will create its own.
     *
     * @return possibly null
     */
    ExecutorService initExecutorService();
}
