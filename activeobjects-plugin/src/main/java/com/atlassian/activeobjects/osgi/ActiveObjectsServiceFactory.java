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
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
// @NotFinalForTesting
public class ActiveObjectsServiceFactory implements ServiceFactory, InitializingBean, DisposableBean
{
    private static final Logger logger = LoggerFactory.getLogger(ActiveObjectsServiceFactory.class);

    private static final String INIT_TASK_TIMEOUT_MS_PROPERTY = "ao-plugin.init.task.timeout";

    @VisibleForTesting
    protected static final int INIT_TASK_TIMEOUT_MS = Integer.getInteger(INIT_TASK_TIMEOUT_MS_PROPERTY, 30000);

    private final EventPublisher eventPublisher;
    private final TenantContext tenantContext;

    @VisibleForTesting
    final ThreadFactory aoContextThreadFactory;

    @VisibleForTesting
    final LoadingCache<Tenant, ExecutorService> initExecutorsByTenant;

    @VisibleForTesting
    volatile boolean destroying = false;

    @VisibleForTesting
    volatile boolean cleaning = false;

    @VisibleForTesting
    final Function<Tenant, ExecutorService> initExecutorFn;

    // use BundleRef to ensure that we key on reference equality of the bundles, not any object equality
    @VisibleForTesting
    final LoadingCache<BundleRef, TenantAwareActiveObjects> aoDelegatesByBundle;

    // note that we need an explicit lock here to allow aoDelegatesByBundle time to load the configuration during the
    // invocation of onPluginModuleEnabledEvent
    @VisibleForTesting
    final Map<Bundle, ActiveObjectsConfiguration> unattachedConfigByBundle = new IdentityHashMap<>();

    private final Lock unattachedConfigsLock = new ReentrantLock();

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
                if (destroying)
                {
                    throw new IllegalStateException("applied initExecutorFn after ActiveObjectsServiceFactory destruction");
                }
                else if (cleaning)
                {
                    throw new IllegalStateException("applied initExecutorFn during ActiveObjects cleaning");
                }

                //noinspection ConstantConditions
                checkNotNull(tenant);
                return initExecutorsByTenant.getUnchecked(tenant);
            }
        };

        // loading cache for ActiveObjects delegates
        aoDelegatesByBundle = CacheBuilder.newBuilder().build(new CacheLoader<BundleRef, TenantAwareActiveObjects>()
        {
            @Override
            public TenantAwareActiveObjects load(@Nonnull final BundleRef bundleRef) throws Exception
            {
                TenantAwareActiveObjects delegate = new TenantAwareActiveObjects(bundleRef.bundle, factory, tenantContext, initExecutorFn);
                delegate.init();
                unattachedConfigsLock.lock();
                try
                {
                    final ActiveObjectsConfiguration aoConfig = unattachedConfigByBundle.get(bundleRef.bundle);
                    if (aoConfig != null)
                    {
                        delegate.setAoConfiguration(aoConfig);
                        unattachedConfigByBundle.remove(bundleRef.bundle);
                    }
                }
                finally
                {
                    unattachedConfigsLock.unlock();
                }
                return delegate;
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        logger.debug("afterPropertiesSet");

        // we want tenant arrival and hot restart event notifications
        eventPublisher.register(this);
    }

    @Override
    public void destroy() throws Exception
    {
        logger.debug("destroy");

        destroying = true;

        for (ExecutorService initExecutor : initExecutorsByTenant.asMap().values())
        {
            initExecutor.shutdownNow();
        }

        for (TenantAwareActiveObjects aoDelegate : aoDelegatesByBundle.asMap().values())
        {
            aoDelegate.destroy();
        }

        eventPublisher.unregister(this);
    }

    /**
     * Invoked when the Gemini Blueprints/Spring DM proxy first accesses the module. Note that Blueprints is lazy, so
     * this may not be called. A "safety backup" is added in {@link #onPluginEnabledEvent(PluginEnabledEvent)} to
     * eagerly initialise the lazy proxies that are not invoked during plugin startup.
     */
    @Override
    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        checkNotNull(bundle);
        logger.debug("getService bundle [{}]", bundle.getSymbolicName());

        if (destroying)
        {
            throw new IllegalStateException("getService after ActiveObjectsServiceFactory destruction");
        }

        return aoDelegatesByBundle.getUnchecked(new BundleRef(bundle));
    }

    /**
     * Invoked when the Gemini Blueprints/Spring DM proxy releases the module. Note that Blueprints is lazy, so the
     * proxy may never have been realised, thus this may not be called. A "safety backup" is added in
     * {@link #onPluginDisabledEvent(PluginDisabledEvent)} to release those references, for plugins that never
     * realise the module.
     */
    @Override
    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object ao)
    {
        checkNotNull(bundle);
        logger.debug("ungetService bundle [{}]", bundle.getSymbolicName());

        aoDelegatesByBundle.invalidate(new BundleRef(bundle));
        if (ao != null && ao instanceof TenantAwareActiveObjects)
        {
            ((TenantAwareActiveObjects) ao).destroy();
        }
    }

    public void startCleaning()
    {
        logger.debug("startCleaning");

        cleaning = true;

        for (final ExecutorService initExecutor : initExecutorsByTenant.asMap().values())
        {
            initExecutor.shutdownNow();
            try
            {
                if (!initExecutor.awaitTermination(INIT_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                {
                    logger.error("startCleaning timed out after {}ms awaiting init thread completion, continuing; note that this timeout may be adjusted via the system property '{}'", INIT_TASK_TIMEOUT_MS, INIT_TASK_TIMEOUT_MS_PROPERTY);
                }
            }
            catch (InterruptedException e)
            {
                logger.error("startCleaning interrupted while awaiting running init thread completion, continuing", e);
            }
        }
    }

    public void stopCleaning()
    {
        logger.debug("stopCleaning");

        cleaning = false;
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
        logger.debug("onTenantArrived tenant arrived {}", tenant);

        if (tenant != null)
        {
            for (TenantAwareActiveObjects aoDelegate : ImmutableList.copyOf(aoDelegatesByBundle.asMap().values()))
            {
                logger.debug("onTenantArrived starting AO delegate for bundle [{}]", aoDelegate.getBundle().getSymbolicName());
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
        logger.debug("onHotRestart performing hot restart with tenant {}", tenant);

        if (tenant != null)
        {
            final ExecutorService initExecutor = initExecutorsByTenant.getIfPresent(tenant);
            initExecutorsByTenant.invalidate(tenant);
            for (TenantAwareActiveObjects aoDelegate : ImmutableList.copyOf(aoDelegatesByBundle.asMap().values()))
            {
                logger.debug("onHotRestart restarting AO delegate for bundle [{}]", aoDelegate.getBundle().getSymbolicName());
                aoDelegate.restartActiveObjects(tenant);
            }

            if (initExecutor != null)
            {
                logger.debug("onHotRestart terminating any initExecutor threads");
                initExecutor.shutdownNow();
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
                if (plugin != null && plugin instanceof OsgiPlugin)
                {
                    final Bundle bundle = ((OsgiPlugin) plugin).getBundle();
                    if (bundle != null)
                    {

                        boolean attachedToDelegate = false;
                        final ActiveObjectsConfiguration aoConfig = ((ActiveObjectModuleDescriptor) moduleDescriptor).getConfiguration();

                        unattachedConfigsLock.lock();
                        try
                        {
                            for (TenantAwareActiveObjects aoDelegate : aoDelegatesByBundle.asMap().values())
                            {
                                if (aoDelegate.getBundle().equals(bundle))
                                {
                                    logger.debug("onPluginModuleEnabledEvent attaching <ao> configuration module to ActiveObjects service of [{}]", plugin);
                                    aoDelegate.setAoConfiguration(aoConfig);
                                    attachedToDelegate = true;
                                    break;
                                }
                            }
                            if (!attachedToDelegate)
                            {
                                logger.debug("onPluginModuleEnabledEvent storing unattached <ao> configuration module for [{}]", plugin);
                                unattachedConfigByBundle.put(bundle, aoConfig);
                            }
                        }
                        finally
                        {
                            unattachedConfigsLock.unlock();
                        }
                    }
                }
            }
        }
    }

    /**
     * Listens for {@link PluginEnabledEvent}. If the plugin is present in the unattached configurations, it will tickle
     * <code>aoDelegatesByBundle</code> to ensure that the configuration has been attached to a service, whether it has
     * been OSGi registered or not.
     */
    @SuppressWarnings ("UnusedDeclaration")
    @EventListener
    public void onPluginEnabledEvent(PluginEnabledEvent pluginEnabledEvent)
    {
        if (pluginEnabledEvent != null)
        {
            final Plugin plugin = pluginEnabledEvent.getPlugin();
            if (plugin != null && plugin instanceof OsgiPlugin)
            {
                final Bundle bundle = ((OsgiPlugin) plugin).getBundle();
                if (bundle != null)
                {
                    if (unattachedConfigByBundle.containsKey(bundle))
                    {
                        logger.debug("onPluginEnabledEvent attaching unbound <ao> to [{}]", plugin);

                        // the cacheloader will do the attaching, after locking first
                        aoDelegatesByBundle.getUnchecked(new BundleRef(bundle));
                    }
                }
            }
        }
    }

    /**
     * Listens for {@link PluginDisabledEvent}. If the plugin is present in the unattached or attached configurations,
     * it will be removed to ensure that we don't leak resources and, more importantly, don't retain it if the plugin
     * is re-enabled.
     */
    @SuppressWarnings ("UnusedDeclaration")
    @EventListener
    public void onPluginDisabledEvent(PluginDisabledEvent pluginDisabledEvent)
    {
        if (pluginDisabledEvent != null)
        {
            final Plugin plugin = pluginDisabledEvent.getPlugin();
            if (plugin != null && plugin instanceof OsgiPlugin)
            {
                final Bundle bundle = ((OsgiPlugin) plugin).getBundle();
                if (bundle != null)
                {
                    logger.debug("onPluginDisabledEvent removing delegate for [{}]", plugin);
                    aoDelegatesByBundle.invalidate(new BundleRef(bundle));

                    unattachedConfigsLock.lock();
                    try
                    {
                        if (unattachedConfigByBundle.containsKey(bundle))
                        {
                            logger.debug("onPluginDisabledEvent removing unbound <ao> for [{}]", plugin);
                            unattachedConfigByBundle.remove(bundle);
                        }
                    }
                    finally
                    {
                        unattachedConfigsLock.unlock();
                    }
                }
            }
        }
    }

    /**
     * Provides a wrapper that gives explicit object identity hashing and reference equality (via identity hasing)of a
     * {@link Bundle}, for use in maps etc.
     */
    protected static class BundleRef
    {
        final Bundle bundle;

        public BundleRef(Bundle bundle)
        {
            this.bundle = checkNotNull(bundle);
        }

        @Override
        public boolean equals(final Object o)
        {
            if (o == null || getClass() != o.getClass()) { return false; }

            final BundleRef bundleRef = (BundleRef) o;

            return bundle == bundleRef.bundle;
        }

        @Override
        public int hashCode()
        {
            return System.identityHashCode(bundle);
        }
    }
}
