package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.ActiveObjectsInitException;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.activeobjects.spi.TransactionSynchronisationManager;
import com.atlassian.activeobjects.util.ActiveObjectsConfigurationServiceProvider;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private final long CONFIGURATION_TIMEOUT_MS = Integer.getInteger("activeobjects.servicefactory.config.timeout", 20000);

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ExecutorService ddlExecutor = Executors
            .newFixedThreadPool(Integer.getInteger("activeobjects.servicefactory.ddl.threadpoolsize", 1),
                new ThreadFactoryBuilder()
                    .setNameFormat("active-objects-ddl-%d")
                    .setDaemon(false)
                    .setPriority(Thread.NORM_PRIORITY + 1).build()); //increased priority as this has the potential to block other threads 

    private final Function<ActiveObjectsKey, DelegatingActiveObjects> makeFromActiveObjectsKey = new Function<ActiveObjectsKey, DelegatingActiveObjects>()
    {
        @Override
        public DelegatingActiveObjects apply(final ActiveObjectsKey key)
        {
            return new DelegatingActiveObjects(submitCreateActiveObjects(key.bundle), key.bundle, tranSyncManager, getDatabaseType());
        }
    };
    
    final Cache<ActiveObjectsKey, DelegatingActiveObjects> aoInstances = CacheBuilder.newBuilder().build(new CacheLoader<ActiveObjectsKey, DelegatingActiveObjects>()
    {
        public DelegatingActiveObjects load(ActiveObjectsKey key) throws Exception 
        {
            return makeFromActiveObjectsKey.apply(key);
        };
    });

    private final ActiveObjectsFactory factory;
    private final ActiveObjectsConfigurationServiceProvider aoConfigurationResolver;
    private final TransactionSynchronisationManager tranSyncManager;
    private final DataSourceProvider dataSourceProvider;
    private final TransactionTemplate transactionTemplate;
    
    public ActiveObjectsServiceFactory(ActiveObjectsFactory factory, ActiveObjectsConfigurationServiceProvider aoConfigurationResolver, EventPublisher eventPublisher, TransactionSynchronisationManager tranSyncManager, DataSourceProvider dataSourceProvider, TransactionTemplate transactionTemplate)
    {
        this.factory = checkNotNull(factory);
        this.aoConfigurationResolver = checkNotNull(aoConfigurationResolver);
        this.tranSyncManager = checkNotNull(tranSyncManager);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.transactionTemplate = checkNotNull(transactionTemplate);
        checkNotNull(eventPublisher).register(this);
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
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
     * Listens for {@link HotRestartEvent} and releases all {@link ActiveObjects instances} flushing their caches.
     */
    @EventListener
    public void onHotRestart(HotRestartEvent hotRestartEvent)
    {
        for (DelegatingActiveObjects ao : ImmutableList.copyOf(aoInstances.asMap().values()))
        {
            ao.restart(submitCreateActiveObjects(ao.getBundle()));
        }
        try
        {
            // wait for the hot restart to complete, this allows callers to know that the restart has complete
            // when the event publisher returns, also helps avoid ddl deadlocks in HSQLDB whilst also staying off
            // the same thread and not joining any existing transactions
            ddlExecutor.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    logger.info("Completed active objects hot restart");
                }
            }).get();
        }
        catch(Exception ex)
        {
            logger.warn("Exception waiting for hot restart to complete", ex);
        }
    }

    /**
     * Creates a delegating active objects that will lazily create the properly configured active objects.
     *
     * @param bundle the bundle for which to create the {@link com.atlassian.activeobjects.external.ActiveObjects}
     * @return an {@link com.atlassian.activeobjects.external.ActiveObjects} instance
     */
    private ActiveObjects createActiveObjects(final ActiveObjectsConfiguration config)
    {
        logger.debug("Creating active object service for plugin {} [{}]", config.getPluginKey());
        return factory.create(config, getDatabaseType());
    };

    /**
     * This is executed in a transaction as some providers create a hibernate session which can only be done in a
     * transaction
     * 
     * @return an enum representing the underlying database type
     */
    private DatabaseType getDatabaseType()
    {
        return transactionTemplate.execute(new TransactionCallback<DatabaseType>()
        {
            @Override
            public DatabaseType doInTransaction()
            {
                return checkNotNull(dataSourceProvider.getDatabaseType(), dataSourceProvider +
                        " returned null for dbType");
            }
        });
    }

    private Promise<ActiveObjects> submitCreateActiveObjects(final Bundle bundle)
    {
        Promise<ActiveObjects> promise = Promises.forFuture(ddlExecutor.submit(new Callable<ActiveObjects>()
        {
            @Override
            public ActiveObjects call() throws Exception
            {
                if(ddlExecutor.isShutdown())
                    throw new InterruptedException("ddlExecutor shutdown, not attempting creation of ActiveObjects for bundle : "+bundle);

                ActiveObjectsConfiguration configuration = aoConfigurationResolver.getAndWait(bundle, CONFIGURATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                return createActiveObjects(configuration);
            }
        })).recover(new Function<Throwable, ActiveObjects>()
        {
            @Override
            public ActiveObjects apply(final Throwable ex)
            {
                Throwables.propagateIfInstanceOf(ex, Error.class);
                Throwables.propagateIfInstanceOf(ex, ActiveObjectsPluginException.class);
                throw new ActiveObjectsInitException("Active Objects failed to initalize for bundle "+bundle.getSymbolicName(), ex);
            }
        });
        return promise;
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
        ddlExecutor.shutdown();
    }
}
