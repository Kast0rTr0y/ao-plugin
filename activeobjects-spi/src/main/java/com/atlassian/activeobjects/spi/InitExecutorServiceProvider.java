package com.atlassian.activeobjects.spi;

import com.atlassian.tenancy.api.Tenant;

import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;

public interface InitExecutorServiceProvider
{
    /**
     * Create an executor service in which to run AO init i.e. DDL and upgrade tasks.
     *
     * {@link com.atlassian.activeobjects.spi.DefaultInitExecutorServiceProvider} provides a standard implementation and
     * should be used unless the product has special snowflake requirements e.g. HSQLDB executing in current thread.

     * @param tenant context for the data source
     */
    @Nonnull
    ExecutorService initExecutorService(@Nonnull Tenant tenant);
}
