package com.atlassian.activeobjects.spi;

import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.tenancy.api.Tenant;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DefaultInitExecutorServiceProvider implements InitExecutorServiceProvider {
    private static final Logger logger = LoggerFactory.getLogger(DefaultInitExecutorServiceProvider.class);

    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    @VisibleForTesting
    final ThreadFactory aoContextThreadFactory;

    public DefaultInitExecutorServiceProvider(final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory) {
        this.threadLocalDelegateExecutorFactory = threadLocalDelegateExecutorFactory;

        // store the CCL of the ao-plugin bundle for use by all shared thread pool executors
        ClassLoader bundleContextClassLoader = Thread.currentThread().getContextClassLoader();
        aoContextThreadFactory = new ContextClassLoaderThreadFactory(bundleContextClassLoader);
    }

    /**
     * Create a thread pool executor with the same context class loader that was used when creating this class.
     *
     * Pool size is <code>activeobjects.servicefactory.ddl.threadpoolsize</code>, default 1.
     *
     * Runs in the same thread local context as the calling code.
     *
     * @param tenant active-objects-init-<tenant.toString()>-%d
     */
    @Nonnull
    @Override
    public ExecutorService initExecutorService(@Nonnull Tenant tenant) {
        logger.debug("creating default init executor");

        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setThreadFactory(aoContextThreadFactory)
                .setNameFormat("active-objects-init-" + tenant.toString() + "-%d")
                .build();

        final ExecutorService delegate = Executors.newFixedThreadPool(Integer.getInteger("activeobjects.servicefactory.ddl.threadpoolsize", 1), threadFactory);

        return threadLocalDelegateExecutorFactory.createExecutorService(delegate);
    }
}
