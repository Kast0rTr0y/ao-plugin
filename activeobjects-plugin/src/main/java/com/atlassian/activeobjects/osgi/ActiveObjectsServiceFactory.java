package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.TenantProvider;
import com.atlassian.activeobjects.spi.ContextClassLoaderThreadFactory;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.activeobjects.spi.InitExecutorServiceProvider;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.tenancy.api.event.TenantArrivedEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
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
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    private final TenantProvider tenantProvider;

    @VisibleForTesting
    final ThreadFactory aoContextThreadFactory;

    @VisibleForTesting
    final ScheduledExecutorService configExecutor;

    @VisibleForTesting
    final LoadingCache<Tenant, ExecutorService> initExecutorsByTenant;

    private final ReadWriteLock initExecutorsLock = new ReentrantReadWriteLock();

    @VisibleForTesting
    volatile boolean initExecutorsShutdown = false;

    @VisibleForTesting
    final Function<Tenant, ExecutorService> initExecutorFn;

    @VisibleForTesting
    final LoadingCache<Bundle, TenantAwareActiveObjectsDelegate> aoDelegatesByBundle;

    public ActiveObjectsServiceFactory(
            @Nonnull final ActiveObjectsFactory factory,
            @Nonnull final EventPublisher eventPublisher,
            @Nonnull final TenantProvider tenantProvider,
            @Nonnull final AOConfigurationGenerator aoConfigurationGenerator,
            @Nonnull final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory,
            @Nonnull final InitExecutorServiceProvider initExecutorServiceProvider)
    {
        this.eventPublisher = checkNotNull(eventPublisher);
        this.tenantProvider = checkNotNull(tenantProvider);
        checkNotNull(factory);
        checkNotNull(aoConfigurationGenerator);
        checkNotNull(threadLocalDelegateExecutorFactory);
        checkNotNull(initExecutorServiceProvider);

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
        aoDelegatesByBundle = CacheBuilder.newBuilder().build(new CacheLoader<Bundle, TenantAwareActiveObjectsDelegate>()
        {
            @Override
            public TenantAwareActiveObjectsDelegate load(@Nonnull final Bundle bundle) throws Exception
            {
                TenantAwareActiveObjectsDelegate delegate = new TenantAwareActiveObjectsDelegate(bundle, factory, tenantProvider, aoConfigurationGenerator, initExecutorFn, configExecutor);
                delegate.init();
                return delegate;
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        // we want tenant arrival and hot restart event notifications
        eventPublisher.register(this);
    }

    @Override
    public void destroy() throws Exception
    {
        logger.debug("destroying");
        configExecutor.shutdown();

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
        Tenant tenant = tenantProvider.getTenant();
        logger.debug("tenant arrived {}", tenant);

        if (tenant != null)
        {
            for (TenantAwareActiveObjectsDelegate aoDelegate : ImmutableList.copyOf(aoDelegatesByBundle.asMap().values()))
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
        Tenant tenant = tenantProvider.getTenant();
        logger.debug("performing hot restart with tenant {}", tenant);

        if (tenant != null)
        {
            for (TenantAwareActiveObjectsDelegate aoDelegate : ImmutableList.copyOf(aoDelegatesByBundle.asMap().values()))
            {
                logger.debug("restarting AO delegate for bundle [{}]", aoDelegate.getBundle().getSymbolicName());
                aoDelegate.restartActiveObjects(tenant);
            }
        }
    }
}
