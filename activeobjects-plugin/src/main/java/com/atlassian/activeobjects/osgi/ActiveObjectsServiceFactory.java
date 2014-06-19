package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.spi.ContextClassLoaderThreadFactory;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.activeobjects.spi.InitExecutorServiceProvider;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReadWriteLock;
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

    public ActiveObjectsServiceFactory(
            @Nonnull final ActiveObjectsFactory factory,
            @Nonnull final EventPublisher eventPublisher,
            @Nonnull final TenantContext tenantContext,
            @Nonnull final AOConfigurationGenerator aoConfigurationGenerator,
            @Nonnull final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory,
            @Nonnull final InitExecutorServiceProvider initExecutorServiceProvider,
            @Nonnull final PluginAccessor pluginAccessor)
    {
        this.eventPublisher = checkNotNull(eventPublisher);
        this.tenantContext = checkNotNull(tenantContext);
        checkNotNull(factory);
        checkNotNull(aoConfigurationGenerator);
        checkNotNull(threadLocalDelegateExecutorFactory);
        checkNotNull(initExecutorServiceProvider);
        checkNotNull(pluginAccessor);

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
                TenantAwareActiveObjects delegate = new TenantAwareActiveObjects(bundle, factory, tenantContext, aoConfigurationGenerator, initExecutorFn, pluginAccessor);
                delegate.init();
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
        logger.debug("bundle [{}]", bundle.getSymbolicName());
        return aoDelegatesByBundle.getUnchecked(bundle);
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object ao)
    {
        aoDelegatesByBundle.invalidate(bundle);
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
            for (TenantAwareActiveObjects aoDelegate : ImmutableList.copyOf(aoDelegatesByBundle.asMap().values()))
            {
                logger.debug("starting AO delegate for bundle [{}]", aoDelegate.getBundle().getSymbolicName());
                aoDelegate.startActiveObjects(tenant);
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
            for (TenantAwareActiveObjects aoDelegate : ImmutableList.copyOf(aoDelegatesByBundle.asMap().values()))
            {
                logger.debug("restarting AO delegate for bundle [{}]", aoDelegate.getBundle().getSymbolicName());
                aoDelegate.restartActiveObjects(tenant);
            }
        }
    }

    /**
     * Listens for {@link PluginEnabledEvent} for propagation to all delegates.
     *
     * If a plugin is enabled before the delegate has made it into the loading cache, it's OK because the delegate
     * does a check for the plugin (and its modules) during {@link TenantAwareActiveObjects#init()} anyway.
     */
    @SuppressWarnings ("UnusedDeclaration")
    @EventListener
    public void onPluginEnabledEvent(PluginEnabledEvent pluginEnabledEvent)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("onPluginEnabledEvent ");
        if (pluginEnabledEvent != null)
        {
            Plugin plugin = pluginEnabledEvent.getPlugin();
            if (plugin != null)
            {
                sb.append(plugin.getKey());
                for (TenantAwareActiveObjects aoDelegate : ImmutableList.copyOf(aoDelegatesByBundle.asMap().values()))
                {
                    aoDelegate.retrieveConfiguration(plugin);
                }
            }
        }
        logger.warn(sb.toString());
    }
}
