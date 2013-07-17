package com.atlassian.activeobjects.osgi;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.java.ao.RawEntity;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.NameConverters;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.config.ActiveObjectsConfigurationFactory;
import com.atlassian.activeobjects.config.PluginKey;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.TransactionManager;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.activeobjects.util.ActiveObjectsConfigurationServiceProvider;
import com.atlassian.activeobjects.util.ServiceTrackerHolder;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.PluginException;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.util.concurrent.Promises;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

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
    private final long CONFIGURATION_TIMEOUT_MS = Integer.getInteger("activeobjects.servicefactory.config.timeout", 10000);

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private final ExecutorService ddlExecutor = Executors
            .newFixedThreadPool(Integer.getInteger("activeobjects.servicefactory.ddl.threadpoolsize", 1),
                new ThreadFactoryBuilder()
                    .setNameFormat("active-objects-ddl-%d")
                    .setDaemon(false)
                    .setPriority(Thread.NORM_PRIORITY + 1).build()); //increased priority as this has the 

    final Function<ActiveObjectsKey, DelegatingActiveObjects> makeFromActiveObjectsKey = new Function<ActiveObjectsKey, DelegatingActiveObjects>()
    {
        @Override
        public DelegatingActiveObjects apply(final ActiveObjectsKey key)
        {
            return new DelegatingActiveObjects(Promises.forFuture(ddlExecutor.submit(new Callable<ActiveObjects>()
            {
                @Override
                public ActiveObjects call() throws Exception
                {
                    return createActiveObjects(key.bundle);
                }
            })));
        }
    };
    
    final Map<ActiveObjectsKey, DelegatingActiveObjects> aoInstances = new MapMaker().makeComputingMap(makeFromActiveObjectsKey);

    private final ActiveObjectsFactory factory;
    private final ActiveObjectsConfigurationServiceProvider aoConfigurationResolver;
    private final TransactionTemplate transactionTemplate;
    
    public ActiveObjectsServiceFactory(ActiveObjectsFactory factory, ActiveObjectsConfigurationServiceProvider aoConfigurationResolver, EventPublisher eventPublisher, TransactionTemplate transactionTemplate)
    {
        this.factory = checkNotNull(factory);
        this.aoConfigurationResolver = checkNotNull(aoConfigurationResolver);
        this.transactionTemplate = checkNotNull(transactionTemplate);
        checkNotNull(eventPublisher).register(this);
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        return aoInstances.get(new ActiveObjectsKey(bundle));
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object ao)
    {
        try
        {
            final ActiveObjects removed = aoInstances.remove(new ActiveObjectsKey(bundle));
            if (removed != null)
            {
                checkState(ao == removed);

                //we can't flush cache because some dependencies may have been de-registered already.
                //removed.flushAll(); // clear all caches for good measure
            }
            else
            {
                logger.warn("Didn't find Active Objects instance matching {}, this shouldn't be happening!", bundle);
            }
        }
        catch (Exception e)
        {
            throw new ActiveObjectsPluginException("An exception occurred un-getting the AO service for bundle " + bundle + ". This could lead to memory leaks!", e);
        }
    }

    /**
     * Listens for {@link HotRestartEvent} and releases all {@link ActiveObjects instances} flushing their caches.
     */
    @EventListener
    public void onHotRestart(HotRestartEvent hotRestartEvent)
    {
        for (DelegatingActiveObjects ao : ImmutableList.copyOf(aoInstances.values()))
        {
            ao.shutdown();
        }
        aoInstances.clear();
    }

    /**
     * Creates a delegating active objects that will lazily create the properly configured active objects.
     *
     * @param bundle the bundle for which to create the {@link com.atlassian.activeobjects.external.ActiveObjects}
     * @return an {@link com.atlassian.activeobjects.external.ActiveObjects} instance
     */
    private ActiveObjects createActiveObjects(final Bundle bundle)
    {
        return transactionTemplate.execute(new TransactionCallback<ActiveObjects>()
        {   
            @Override
            public ActiveObjects doInTransaction()
            {
                try
                {
                    logger.debug("Creating active object service for bundle {} [{}]", bundle.getSymbolicName(), bundle.getBundleId());
                    return factory.create(aoConfigurationResolver.getAndWait(bundle, CONFIGURATION_TIMEOUT_MS));
                }
                catch(InterruptedException ie)
                {
                    throw new RuntimeException(ie);
                }
            }
        });
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
