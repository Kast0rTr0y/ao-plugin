package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.plugin.ActiveObjectModuleDescriptor;
import com.atlassian.activeobjects.spi.ContextClassLoaderThreadFactory;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.activeobjects.spi.InitExecutorServiceProvider;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.tenancy.api.TenantContext;
import com.atlassian.tenancy.api.event.TenantArrivedEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
public final class ActiveObjectsServiceFactory implements ServiceFactory, InitializingBean, DisposableBean
{
    private static final Logger logger = LoggerFactory.getLogger(ActiveObjectsServiceFactory.class);

    private final EventPublisher eventPublisher;
    private final TenantContext tenantContext;

    @VisibleForTesting
    final ThreadFactory aoContextThreadFactory;

    @VisibleForTesting
    final LoadingCache<Tenant, ExecutorService> initExecutorsByTenant;

    private final ReadWriteLock initExecutorsLock = new ReentrantReadWriteLock();

    @VisibleForTesting
    volatile boolean initExecutorsShutdown = false;

    @VisibleForTesting
    final Function<Tenant, ExecutorService> initExecutorFn;

    @VisibleForTesting
    final LoadingCache<Bundle, TenantAwareActiveObjects> aoDelegatesByBundle;

    @VisibleForTesting
    final Map<String, ActiveObjectsConfiguration> unattachedConfigByKey = new HashMap<String, ActiveObjectsConfiguration>();

    private final Lock delegateConfigLock = new ReentrantLock();

    public ActiveObjectsServiceFactory(
            @Nonnull final ActiveObjectsFactory factory,
            @Nonnull final EventPublisher eventPublisher,
            @Nonnull final TenantContext tenantContext,
            @Nonnull final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory,
            @Nonnull final InitExecutorServiceProvider initExecutorServiceProvider)
    {
        this.eventPublisher = checkNotNull(eventPublisher);
        this.tenantContext = checkNotNull(tenantContext);
        checkNotNull(factory);
        checkNotNull(threadLocalDelegateExecutorFactory);
        checkNotNull(initExecutorServiceProvider);

        // store the CCL of the ao-plugin bundle for use by all shared thread pool executors
        ClassLoader bundleContextClassLoader = Thread.currentThread().getContextClassLoader();
        aoContextThreadFactory = new ContextClassLoaderThreadFactory(bundleContextClassLoader);

        // loading cache for init executors pools
        initExecutorsByTenant = CacheBuilder.newBuilder().build(new CacheLoader<Tenant, ExecutorService>()
        {
            @Override
            public ExecutorService load(@Nonnull final Tenant tenant) throws Exception
            {
                logger.debug("creating new init executor for {}", tenant);
                return initExecutorServiceProvider.initExecutorService(tenant);
            }
        });

        // initExecutor retrieval function
        initExecutorFn = new Function<Tenant, ExecutorService>()
        {
            @Override
            public ExecutorService apply(@Nullable final Tenant tenant)
            {
                initExecutorsLock.readLock().lock();
                try
                {
                    if (initExecutorsShutdown)
                    {
                        throw new IllegalStateException("applied initExecutorFn after ActiveObjectsServiceFactory destruction");
                    }

                    //noinspection ConstantConditions
                    checkNotNull(tenant);
                    return initExecutorsByTenant.getUnchecked(tenant);
                }
                finally
                {
                    initExecutorsLock.readLock().unlock();
                }
            }
        };

        // loading cache for ActiveObjects delegates
        aoDelegatesByBundle = CacheBuilder.newBuilder().build(new CacheLoader<Bundle, TenantAwareActiveObjects>()
        {
            @Override
            public TenantAwareActiveObjects load(@Nonnull final Bundle bundle) throws Exception
            {
                TenantAwareActiveObjects delegate = new TenantAwareActiveObjects(bundle, factory, tenantContext, initExecutorFn);
                delegate.init();
                delegateConfigLock.lock();
                try
                {
                    final ActiveObjectsConfiguration aoConfig = unattachedConfigByKey.get(bundle.getSymbolicName());
                    if (aoConfig != null)
                    {
                        delegate.setAoConfiguration(aoConfig);
                        unattachedConfigByKey.remove(bundle.getSymbolicName());
                    }
                }
                finally
                {
                    delegateConfigLock.unlock();
                }
                return delegate;
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        logger.warn("afterPropertiesSet");

        // we want tenant arrival and hot restart event notifications
        eventPublisher.register(this);
    }

    @Override
    public void destroy() throws Exception
    {
        logger.warn("destroying");

        initExecutorsLock.writeLock().lock();
        try
        {
            for (ExecutorService initExecutor : ImmutableList.copyOf(initExecutorsByTenant.asMap().values()))
            {
                initExecutor.shutdownNow();
            }
            initExecutorsShutdown = true;
        }
        finally
        {
            initExecutorsLock.writeLock().unlock();
        }

        eventPublisher.unregister(this);
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        checkNotNull(bundle);
        logger.warn("bundle [{}]", bundle.getSymbolicName());

        delegateConfigLock.lock();
        try
        {
            return aoDelegatesByBundle.getUnchecked(bundle);
        }
        finally
        {
            delegateConfigLock.unlock();
        }
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object ao)
    {
        delegateConfigLock.lock();
        try
        {
            aoDelegatesByBundle.invalidate(bundle);
        }
        finally
        {
            delegateConfigLock.unlock();
        }
    }

    /**
     * Listens for {@link TenantArrivedEvent} and allows initialisation of any uninitialised instances
     */
    @SuppressWarnings ("UnusedDeclaration")
    @EventListener
    public void onTenantArrived(TenantArrivedEvent event)
    {
        // ensure that the tenant is still present
        Tenant tenant = tenantContext.getCurrentTenant();
        logger.debug("tenant arrived {}", tenant);

        if (tenant != null)
        {
            delegateConfigLock.lock();
            try
            {

                for (TenantAwareActiveObjects aoDelegate : aoDelegatesByBundle.asMap().values())
                {
                    logger.debug("starting AO delegate for bundle [{}]", aoDelegate.getBundle().getSymbolicName());
                    aoDelegate.startActiveObjects(tenant);
                }
            }
            finally
            {
                delegateConfigLock.unlock();
            }
        }
    }

    /**
     * Listens for {@link HotRestartEvent} and recreate all {@link ActiveObjects} instances within the delegates with
     * the possibly different configuration and data source
     */
    @SuppressWarnings ("UnusedDeclaration")
    @EventListener
    public void onHotRestart(HotRestartEvent hotRestartEvent)
    {
        Tenant tenant = tenantContext.getCurrentTenant();
        logger.debug("performing hot restart with tenant {}", tenant);

        if (tenant != null)
        {
            delegateConfigLock.lock();
            try
            {

                for (TenantAwareActiveObjects aoDelegate : aoDelegatesByBundle.asMap().values())
                {
                    logger.debug("restarting AO delegate for bundle [{}]", aoDelegate.getBundle().getSymbolicName());
                    aoDelegate.restartActiveObjects(tenant);
                }
            }
            finally
            {
                delegateConfigLock.unlock();
            }
        }
    }

    /**
     * Listens for {@link PluginModuleEnabledEvent} for {@link ActiveObjectModuleDescriptor}.
     * Passes it to appropriate delegate i.e. the one for which the plugin/bundle key matches.
     */
    @SuppressWarnings ("UnusedDeclaration")
    @EventListener
    public void onPluginModuleEnabledEvent(PluginModuleEnabledEvent pluginModuleEnabledEvent)
    {
        if (pluginModuleEnabledEvent != null)
        {
            final ModuleDescriptor moduleDescriptor = pluginModuleEnabledEvent.getModule();
            if (moduleDescriptor != null && moduleDescriptor instanceof ActiveObjectModuleDescriptor)
            {
                final Plugin plugin = moduleDescriptor.getPlugin();
                if (plugin != null)
                {
                    final String pluginKey = plugin.getKey();
                    if (pluginKey != null)
                    {
                        boolean attachedToDelegate = false;
                        ActiveObjectsConfiguration aoConfig = ((ActiveObjectModuleDescriptor) moduleDescriptor).getConfiguration();

                        delegateConfigLock.lock();
                        try
                        {
                            for (TenantAwareActiveObjects aoDelegate : aoDelegatesByBundle.asMap().values())
                            {
                                if (pluginKey.equals(aoDelegate.getBundle().getSymbolicName()))
                                {
                                    aoDelegate.setAoConfiguration(aoConfig);
                                    attachedToDelegate = true;
                                    break;
                                }
                            }
                            if (!attachedToDelegate)
                            {
                                unattachedConfigByKey.put(pluginKey, aoConfig);
                            }
                        }
                        finally
                        {
                            delegateConfigLock.lock();
                        }
                    }
                }
            }
        }
    }
}
