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
import com.atlassian.tenancy.api.TenantAccessor;
import com.atlassian.tenancy.api.event.TenantArrivedEvent;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.SettableFuture;
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

    private final SettableFuture<Void> dbAvailableFuture = SettableFuture.create();

    private final ActiveObjectsFactory factory;
    private final ActiveObjectsConfigurationServiceProvider aoConfigurationResolver;
    private final DataSourceProvider dataSourceProvider;
    private final TransactionTemplate transactionTemplate;
    private final EventPublisher eventPublisher;
    private final TenantAccessor tenantAccessor;

    // all DDL and upgrade tasks will execute in this single thread
    private final ExecutorService initExecutor = Executors.newFixedThreadPool(Integer.getInteger("activeobjects.servicefactory.ddl.threadpoolsize", 1),
            new ThreadFactoryBuilder()
                    .setNameFormat("active-objects-init-%d")
                    .setDaemon(false)
                    .setPriority(Thread.NORM_PRIORITY + 1).build()
    );;

    final LoadingCache<ActiveObjectsKey, BabyBearActiveObjectsDelegate> aoInstances = CacheBuilder.newBuilder().build(new CacheLoader<ActiveObjectsKey, BabyBearActiveObjectsDelegate>()
    {
        @Override
        public BabyBearActiveObjectsDelegate load(final ActiveObjectsKey key) throws Exception
        {
            return new BabyBearActiveObjectsDelegate(checkDbAvailability, dbAvailableFuture, key.bundle, factory, aoConfigurationResolver, dataSourceProvider, transactionTemplate, initExecutor);
        }
    });

    private final Function<Void, Boolean> checkDbAvailability = new Function<Void, Boolean>()
    {
        @Override
        public Boolean apply(@Nullable final Void input)
        {
            boolean tenantAvailable = tenantAccessor.getAvailableTenants().iterator().hasNext();
            if (tenantAvailable)
            {
                dbAvailableFuture.set(null);
            }
            return tenantAvailable;
        }
    };

    public ActiveObjectsServiceFactory(ActiveObjectsFactory factory, ActiveObjectsConfigurationServiceProvider aoConfigurationResolver, EventPublisher eventPublisher, DataSourceProvider dataSourceProvider, TransactionTemplate transactionTemplate, TenantAccessor tenantAccessor)
    {
        this.factory = checkNotNull(factory);
        this.aoConfigurationResolver = checkNotNull(aoConfigurationResolver);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.transactionTemplate = checkNotNull(transactionTemplate);
        this.eventPublisher = checkNotNull(eventPublisher);
        this.tenantAccessor = checkNotNull(tenantAccessor);

        // check if the DB is available right now
        boolean dbAvailable = checkDbAvailability.apply(null);
        logger.debug("dbAvailable={}", dbAvailable);

        // we want tenant arrival and hot restart event notifications
        eventPublisher.register(this);
    }

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
        logger.debug("making DB available to instances");
        checkDbAvailability.apply(null);
    }

    /**
     * Listens for {@link HotRestartEvent} and recreate all {@link ActiveObjects} instances within the delegates with
     * the new configuration and data source
     */
    @EventListener
    public void onHotRestart(HotRestartEvent hotRestartEvent)
    {
        for (BabyBearActiveObjectsDelegate aoInstance : ImmutableList.copyOf(aoInstances.asMap().values()))
        {
            logger.debug("restarting AO delegate for bundle [{}]", aoInstance.getBundle().getSymbolicName());
            aoInstance.restart();
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
        eventPublisher.unregister(this);
    }
}
