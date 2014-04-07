package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.ActiveObjectsInitException;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.activeobjects.util.ActiveObjectsConfigurationServiceProvider;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.tenancy.api.TenantAccessor;
import com.atlassian.tenancy.api.event.TenantArrivedEvent;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
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
    private final ActiveObjectsConfigurationServiceProvider aoConfigurationResolver;
    private final DataSourceProvider dataSourceProvider;
    private final TransactionTemplate transactionTemplate;
    private final EventPublisher eventPublisher;
    private final TenantAccessor tenantAccessor;

    public ActiveObjectsServiceFactory(ActiveObjectsFactory factory, ActiveObjectsConfigurationServiceProvider aoConfigurationResolver, EventPublisher eventPublisher, DataSourceProvider dataSourceProvider, TransactionTemplate transactionTemplate, TenantAccessor tenantAccessor)
    {
        this.factory = checkNotNull(factory);
        this.aoConfigurationResolver = checkNotNull(aoConfigurationResolver);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.transactionTemplate = checkNotNull(transactionTemplate);
        this.eventPublisher = checkNotNull(eventPublisher);
        this.tenantAccessor = checkNotNull(tenantAccessor);

        // we want tenant arrival and hot restart event notifications
        eventPublisher.register(this);
    }

    final Supplier<DataSource> dataSourceSupplier = new Supplier<DataSource>()
    {
        @Override
        public DataSource get()
        {

            // stubbed until this is available from {@link com.atlassian.tenancy.api.TenantAccessor}
            if (tenantAccessor.getAvailableTenants().iterator().hasNext())
            {
                return new DataSource(tenantAccessor.getAvailableTenants().iterator().next());
            }
            else
            {
                return null;
            }
        }
    };

    private final LoadingCache<DataSource, ExecutorService> initExecutors = CacheBuilder.newBuilder().build(new CacheLoader<DataSource, ExecutorService>()
    {
        @Override
        public ExecutorService load(final DataSource key) throws Exception
        {
            logger.debug("loading new init executor for {}", key);
            return Executors.newFixedThreadPool(Integer.getInteger("activeobjects.servicefactory.ddl.threadpoolsize", 1),
                    new ThreadFactoryBuilder()
                            .setNameFormat("active-objects-init-" + key.toString() + "-%d")
                            .setDaemon(false)
                            .setPriority(Thread.NORM_PRIORITY + 1).build()
            );
        }
    });

    private final Supplier<ExecutorService> initExecutorSupplier = new Supplier<ExecutorService>()
    {
        @Override
        public ExecutorService get()
        {
            return initExecutors.getUnchecked(dataSourceSupplier.get());
        }
    };

    final LoadingCache<ActiveObjectsKey, BabyBearActiveObjectsDelegate> aoInstances = CacheBuilder.newBuilder().build(new CacheLoader<ActiveObjectsKey, BabyBearActiveObjectsDelegate>()
    {
        @Override
        public BabyBearActiveObjectsDelegate load(final ActiveObjectsKey key) throws Exception
        {
            return new BabyBearActiveObjectsDelegate(key.bundle, factory, aoConfigurationResolver, dataSourceProvider, transactionTemplate, dataSourceSupplier, initExecutorSupplier);
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
        logger.debug("event={}", event);
        for (BabyBearActiveObjectsDelegate aoInstance : ImmutableList.copyOf(aoInstances.asMap().values()))
        {
            logger.debug("starting AO delegate for bundle [{}]", aoInstance.getBundle().getSymbolicName());
            aoInstance.startActiveObjects(dataSourceSupplier.get());
        }
    }

    /**
     * Listens for {@link HotRestartEvent} and recreate all {@link ActiveObjects} instances within the delegates with
     * the possibly different configuration and data source
     */
    @EventListener
    public void onHotRestart(HotRestartEvent hotRestartEvent)
    {
        for (BabyBearActiveObjectsDelegate aoInstance : ImmutableList.copyOf(aoInstances.asMap().values()))
        {
            logger.debug("restarting AO delegate for bundle [{}]", aoInstance.getBundle().getSymbolicName());
            aoInstance.restartActiveObjects(dataSourceSupplier.get());
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
        for (ExecutorService initExecutor : ImmutableList.copyOf(initExecutors.asMap().values()))
        {
            initExecutor.shutdown();
        }
        eventPublisher.unregister(this);
    }

    class DataSource
    {
        private final Tenant tenant;

        DataSource(final Tenant tenant)
        {
            this.tenant = tenant;
        }

        @Override
        public String toString()
        {
            return "DataSource{" +
                    "tenant=" + tenant +
                    '}';
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            final DataSource that = (DataSource) o;

            if (tenant != null ? !tenant.equals(that.tenant) : that.tenant != null) { return false; }

            return true;
        }

        @Override
        public int hashCode()
        {
            return tenant != null ? tenant.hashCode() : 0;
        }
    }
}
