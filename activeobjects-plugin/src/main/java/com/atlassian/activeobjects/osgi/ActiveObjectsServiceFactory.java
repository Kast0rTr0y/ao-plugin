package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.ActiveObjectsInitException;
import com.atlassian.activeobjects.internal.TenantProvider;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.activeobjects.spi.ExecutorServiceProvider;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.tenancy.api.event.TenantArrivedEvent;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>This is the service factory that will create the {@link com.atlassian.activeobjects.external.ActiveObjects}
 * instance for each plugin using active objects.</p>
 *
 * <p>The instance created by that factory is a delegating instance that works together with the
 * {@link ActiveObjectsServiceFactory} to get a correctly configure instance according
 * to the {@link com.atlassian.activeobjects.config.ActiveObjectsConfiguration plugin configuration} and
 * the application configuration.</p>
 */
public final class ActiveObjectsServiceFactory implements ServiceFactory, DisposableBean
{
    private static final Logger logger = LoggerFactory.getLogger(ActiveObjectsServiceFactory.class);

    private final ActiveObjectsFactory factory;
    private final DataSourceProvider dataSourceProvider;
    private final TransactionTemplate transactionTemplate;
    private final EventPublisher eventPublisher;
    private final TenantProvider tenantProvider;
    private final AOConfigurationServiceProvider configurationServiceProvider;
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;
    private final ExecutorServiceProvider executorServiceProvider;

    private final ThreadFactory aoContextThreadFactory;

    private final ScheduledExecutorService configExecutor;

    public ActiveObjectsServiceFactory(
            @Nonnull final ActiveObjectsFactory factory,
            @Nonnull final EventPublisher eventPublisher,
            @Nonnull final DataSourceProvider dataSourceProvider,
            @Nonnull final TransactionTemplate transactionTemplate,
            @Nonnull final TenantProvider tenantProvider,
            @Nonnull final AOConfigurationServiceProvider configurationServiceProvider,
            @Nonnull final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory,
            @Nonnull final ExecutorServiceProvider executorServiceProvider)
    {
        this.factory = checkNotNull(factory);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.transactionTemplate = checkNotNull(transactionTemplate);
        this.eventPublisher = checkNotNull(eventPublisher);
        this.tenantProvider = checkNotNull(tenantProvider);
        this.configurationServiceProvider = checkNotNull(configurationServiceProvider);
        this.threadLocalDelegateExecutorFactory = checkNotNull(threadLocalDelegateExecutorFactory);
        this.executorServiceProvider = checkNotNull(executorServiceProvider);

        // store the CCL of the ao-plugin bundle for use by all shared thread pool executors
        ClassLoader bundleContextClassLoader = Thread.currentThread().getContextClassLoader();
        aoContextThreadFactory = new ContextClassLoaderThreadFactory(bundleContextClassLoader);

        // scheduled single thread pool for use by AO plugins waiting for config modules
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setThreadFactory(aoContextThreadFactory)
                .setNameFormat("active-objects-config")
                .build();
        final ScheduledExecutorService delegate = Executors.newSingleThreadScheduledExecutor(threadFactory);
        configExecutor = threadLocalDelegateExecutorFactory.createScheduledExecutorService(delegate);

        // we want tenant arrival and hot restart event notifications
        eventPublisher.register(this);
    }

    private final LoadingCache<Tenant, ExecutorService> initExecutors = CacheBuilder.newBuilder().build(new CacheLoader<Tenant, ExecutorService>()
    {
        @Override
        public ExecutorService load(final Tenant tenant) throws Exception
        {
            logger.debug("loading new init executor for {}", tenant);

            ExecutorService snowflakeExecutorService = executorServiceProvider.initExecutorService();
            if (snowflakeExecutorService != null)
            {
                logger.debug("using snowflake executor service provided by the application");
                return snowflakeExecutorService;
            }
            else
            {
                logger.debug("creating new init thread pool and executor service");

                // create a thread pool just for DDL, update etc.
                final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                        .setThreadFactory(aoContextThreadFactory)
                        .setNameFormat("active-objects-init-" + tenant.toString() + "-%d")
                        .build();
                final ExecutorService delegate = Executors.newFixedThreadPool(Integer.getInteger("activeobjects.servicefactory.ddl.threadpoolsize", 1), threadFactory);

                return threadLocalDelegateExecutorFactory.createExecutorService(delegate);
            }
        }
    });

    private final Function<Tenant, ExecutorService> initExecutorFunction = new Function<Tenant, ExecutorService>()
    {
        @Override
        public ExecutorService apply(@Nullable final Tenant tenant)
        {
            checkNotNull(tenant);
            return initExecutors.getUnchecked(tenant);
        }
    };

    final LoadingCache<ActiveObjectsKey, DelegatingActiveObjects> aoInstances = CacheBuilder.newBuilder().build(new CacheLoader<ActiveObjectsKey, DelegatingActiveObjects>()
    {
        @Override
        public DelegatingActiveObjects load(final ActiveObjectsKey key) throws Exception
        {
            return new DelegatingActiveObjects(key.bundle, factory, dataSourceProvider, transactionTemplate, tenantProvider, configurationServiceProvider, initExecutorFunction, configExecutor);
        }
    });

    @Override
    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        logger.debug("bundle [{}]", bundle.getSymbolicName());
        try
        {
            return aoInstances.get(new ActiveObjectsKey(bundle));
        }
        catch (ExecutionException e)
        {
            if(ActiveObjectsInitException.class.isAssignableFrom(e.getCause().getClass()))
            {
                throw (ActiveObjectsInitException)e.getCause();
            }
            throw new ActiveObjectsInitException("Error retrieving active objects for bundle "+bundle.getSymbolicName(),e);
        }
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object ao)
    {
        aoInstances.invalidate(new ActiveObjectsKey(bundle));
    }

    /**
     * Listens for {@link TenantArrivedEvent} and allows initialisation of any uninitialised instances
     */
    @EventListener
    public void onTenantArrived(TenantArrivedEvent event)
    {
        Tenant tenant = tenantProvider.getTenant();
        logger.debug("tenant arrived {}", tenant);

        if (tenant != null)
        {
            for (DelegatingActiveObjects aoInstance : ImmutableList.copyOf(aoInstances.asMap().values()))
            {
                logger.debug("starting AO delegate for bundle [{}]", aoInstance.getBundle().getSymbolicName());
                aoInstance.startActiveObjects(tenant);
            }
        }
    }

    /**
     * Listens for {@link HotRestartEvent} and recreate all {@link ActiveObjects} instances within the delegates with
     * the possibly different configuration and data source
     */
    @EventListener
    public void onHotRestart(HotRestartEvent hotRestartEvent)
    {
        Tenant tenant = tenantProvider.getTenant();
        logger.debug("performing hot restart with tenant {}", tenant);

        if (tenant != null)
        {
            for (DelegatingActiveObjects aoInstance : ImmutableList.copyOf(aoInstances.asMap().values()))
            {
                logger.debug("restarting AO delegate for bundle [{}]", aoInstance.getBundle().getSymbolicName());
                aoInstance.restartActiveObjects(tenant);
            }
        }
    }

    private static final class ActiveObjectsKey
    {
        public final Bundle bundle;

        private ActiveObjectsKey(Bundle bundle)
        {
            this.bundle = checkNotNull(bundle);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            final ActiveObjectsKey that = (ActiveObjectsKey) o;

            return this.bundle.getBundleId() == that.bundle.getBundleId();
        }

        @Override
        public int hashCode()
        {
            return ((Long) bundle.getBundleId()).hashCode();
        }
    }

    @Override
    public void destroy() throws Exception
    {
        logger.debug("destroying");
        configExecutor.shutdown();
        for (ExecutorService initExecutor : ImmutableList.copyOf(initExecutors.asMap().values()))
        {
            initExecutor.shutdown();
        }
        eventPublisher.unregister(this);
    }
}
