package com.atlassian.activeobjects.spi;

import java.util.concurrent.ExecutorService;

public interface InitExecutorServiceProvider
{
    /**
     * Create an executor service in which to run AO init i.e. DDL and upgrade tasks.
     *
     * {@link com.atlassian.activeobjects.spi.DefaultInitExecutorServiceProvider} provides a standard implementation and
     * should be used unless the product has special snowflake requirements e.g. HSQLDB executing in current thread.

     * @param name optional tag that may be used for thread naming
     */
    ExecutorService initExecutorService(String name);
}
