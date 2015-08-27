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
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
public class ActiveObjectsServiceFactory implements ServiceFactory, InitializingBean, DisposableBean {
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

    @VisibleForTesting
    final LoadingCache<Bundle, TenantAwareActiveObjects> aoDelegatesByBundle;

    @VisibleForTesting
    final Map<String, ActiveObjectsConfiguration> unattachedConfigByPluginKey = new HashMap<String, ActiveObjectsConfiguration>();

    private final Lock unattachedConfigsLock = new ReentrantLock();

    public ActiveObjectsServiceFactory(
            @Nonnull final ActiveObjectsFactory factory,
            @Nonnull final EventPublisher eventPublisher,
            @Nonnull final TenantContext tenantContext,
            @Nonnull final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory,
            @Nonnull final InitExecutorServiceProvider initExecutorServiceProvider) {
        this.eventPublisher = checkNotNull(eventPublisher);
        this.tenantContext = checkNotNull(tenantContext);
        checkNotNull(factory);
        checkNotNull(threadLocalDelegateExecutorFactory);
        checkNotNull(initExecutorServiceProvider);

        // store the CCL of the ao-plugin bundle for use by all shared thread pool executors
        ClassLoader bundleContextClassLoader = Thread.currentThread().getContextClassLoader();
        aoContextThreadFactory = new ContextClassLoaderThreadFactory(bundleContextClassLoader);

        // loading cache for init executors pools
        initExecutorsByTenant = CacheBuilder.newBuilder().build(new CacheLoader<Tenant, ExecutorService>() {
            @Override
            public ExecutorService load(@Nonnull final Tenant tenant) throws Exception {
                logger.debug("creating new init executor for {}", tenant);
                return initExecutorServiceProvider.initExecutorService(tenant);
            }
        });

        // initExecutor retrieval function
        initExecutorFn = new Function<Tenant, ExecutorService>() {
            @Override
            public ExecutorService apply(@Nullable final Tenant tenant) {
                if (destroying) {
                    throw new IllegalStateException("applied initExecutorFn after ActiveObjectsServiceFactory destruction");
                } else if (cleaning) {
                    throw new IllegalStateException("applied initExecutorFn during ActiveObjects cleaning");
                }

                //noinspection ConstantConditions
                checkNotNull(tenant);
                return initExecutorsByTenant.getUnchecked(tenant);
            }
        };

        // loading cache for ActiveObjects delegates
        aoDelegatesByBundle = CacheBuilder.newBuilder().build(new CacheLoader<Bundle, TenantAwareActiveObjects>() {
            @Override
            public TenantAwareActiveObjects load(@Nonnull final Bundle bundle) throws Exception {
                TenantAwareActiveObjects delegate = new TenantAwareActiveObjects(bundle, factory, tenantContext, initExecutorFn);
                delegate.init();
                unattachedConfigsLock.lock();
                try {
                    final String pluginKey = OsgiHeaderUtil.getPluginKey(bundle);
                    final ActiveObjectsConfiguration aoConfig = unattachedConfigByPluginKey.get(pluginKey);
                    if (aoConfig != null) {
                        delegate.setAoConfiguration(aoConfig);
                        unattachedConfigByPluginKey.remove(pluginKey);
                    }
                } finally {
                    unattachedConfigsLock.unlock();
                }
                return delegate;
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.debug("afterPropertiesSet");

        // we want tenant arrival and hot restart event notifications
        eventPublisher.register(this);
    }

    @Override
    public void destroy() throws Exception {
        logger.debug("destroying");

        destroying = true;

        for (ExecutorService initExecutor : initExecutorsByTenant.asMap().values()) {
            initExecutor.shutdownNow();
        }

        for (TenantAwareActiveObjects aoDelegate : aoDelegatesByBundle.asMap().values()) {
            aoDelegate.destroy();
        }

        eventPublisher.unregister(this);
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration) {
        checkNotNull(bundle);
        logger.debug("bundle [{}]", bundle.getSymbolicName());

        if (destroying) {
            throw new IllegalStateException("getService after ActiveObjectsServiceFactory destruction");
        }

        return aoDelegatesByBundle.getUnchecked(bundle);
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object ao) {
        aoDelegatesByBundle.invalidate(bundle);
        if (ao instanceof TenantAwareActiveObjects) {
            ((TenantAwareActiveObjects) ao).destroy();
        }
    }

    public void startCleaning() {
        logger.debug("startCleaning");

        cleaning = true;

        for (final ExecutorService initExecutor : initExecutorsByTenant.asMap().values()) {
            initExecutor.shutdownNow();
            try {
                if (!initExecutor.awaitTermination(INIT_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    logger.error("startCleaning timed out after {}ms awaiting init thread completion, continuing; note that this timeout may be adjusted via the system property '{}'", INIT_TASK_TIMEOUT_MS, INIT_TASK_TIMEOUT_MS_PROPERTY);
                }
            } catch (InterruptedException e) {
                logger.error("startCleaning interrupted while awaiting running init thread completion, continuing", e);
            }
        }
    }

    public void stopCleaning() {
        logger.debug("stopCleaning");

        cleaning = false;
    }

    /**
     * Listens for {@link TenantArrivedEvent} and allows initialisation of any uninitialised instances
     */
    @SuppressWarnings("UnusedDeclaration")
    @EventListener
    public void onTenantArrived(TenantArrivedEvent event) {
        // ensure that the tenant is still present
        Tenant tenant = tenantContext.getCurrentTenant();
        logger.debug("tenant arrived {}", tenant);

        if (tenant != null) {
            for (TenantAwareActiveObjects aoDelegate : ImmutableList.copyOf(aoDelegatesByBundle.asMap().values())) {
                logger.debug("starting AO delegate for bundle [{}]", aoDelegate.getBundle().getSymbolicName());
                aoDelegate.startActiveObjects(tenant);
            }
        }
    }

    /**
     * Listens for {@link HotRestartEvent} and recreate all {@link ActiveObjects} instances within the delegates with
     * the possibly different configuration and data source
     */
    @SuppressWarnings("UnusedDeclaration")
    @EventListener
    public void onHotRestart(HotRestartEvent hotRestartEvent) {
        Tenant tenant = tenantContext.getCurrentTenant();
        logger.debug("performing hot restart with tenant {}", tenant);

        if (tenant != null) {
            final ExecutorService initExecutor = initExecutorsByTenant.getIfPresent(tenant);
            initExecutorsByTenant.invalidate(tenant);
            for (TenantAwareActiveObjects aoDelegate : ImmutableList.copyOf(aoDelegatesByBundle.asMap().values())) {
                logger.debug("restarting AO delegate for bundle [{}]", aoDelegate.getBundle().getSymbolicName());
                aoDelegate.restartActiveObjects(tenant);
            }

            if (initExecutor != null) {
                logger.debug("terminating any initExecutor threads");
                initExecutor.shutdownNow();
            }
        }
    }

    /**
     * Listens for {@link PluginModuleEnabledEvent} for {@link ActiveObjectModuleDescriptor}.
     * Passes it to appropriate delegate i.e. the one for which the plugin/bundle key matches.
     */
    @SuppressWarnings("UnusedDeclaration")
    @EventListener
    public void onPluginModuleEnabledEvent(PluginModuleEnabledEvent pluginModuleEnabledEvent) {
        if (pluginModuleEnabledEvent != null) {
            final ModuleDescriptor moduleDescriptor = pluginModuleEnabledEvent.getModule();
            if (moduleDescriptor != null && moduleDescriptor instanceof ActiveObjectModuleDescriptor) {
                final Plugin plugin = moduleDescriptor.getPlugin();
                if (plugin != null) {
                    final String pluginKey = plugin.getKey();
                    if (pluginKey != null) {
                        boolean attachedToDelegate = false;
                        ActiveObjectsConfiguration aoConfig = ((ActiveObjectModuleDescriptor) moduleDescriptor).getConfiguration();

                        unattachedConfigsLock.lock();
                        try {
                            for (TenantAwareActiveObjects aoDelegate : aoDelegatesByBundle.asMap().values()) {
                                if (pluginKey.equals(OsgiHeaderUtil.getPluginKey(aoDelegate.getBundle()))) {
                                    aoDelegate.setAoConfiguration(aoConfig);
                                    attachedToDelegate = true;
                                    break;
                                }
                            }
                            if (!attachedToDelegate) {
                                unattachedConfigByPluginKey.put(pluginKey, aoConfig);
                            }
                        } finally {
                            unattachedConfigsLock.unlock();
                        }
                    }
                }
            }
        }
    }
}
