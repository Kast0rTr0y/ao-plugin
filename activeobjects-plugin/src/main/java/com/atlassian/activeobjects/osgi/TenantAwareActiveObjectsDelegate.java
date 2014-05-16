package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsModuleMetaData;
import com.atlassian.activeobjects.external.NoDataSourceException;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.ActiveObjectsInitException;
import com.atlassian.activeobjects.internal.TenantProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.SettableFuture;
import net.java.ao.DBParam;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Delegate for {@link com.atlassian.activeobjects.external.ActiveObjects}.
 *
 * Baby bear is not to eager, not to lazy.
 *
 * Any attempt to invoke this when no tenant is present will result in a {@link com.atlassian.activeobjects.external.NoDataSourceException}.
 *
 * Delegate calls will block when DDL / updgrade tasks are running.
 *
 * DDL / upgrade tasks will be initiated by the first call to the delegate or by a call to {@link #startActiveObjects}
 */
class TenantAwareActiveObjectsDelegate implements ActiveObjects, ServiceListener
{
    private static final Logger logger = LoggerFactory.getLogger(TenantAwareActiveObjectsDelegate.class);

    static final String CONFIGURATION_TIMEOUT_MS_PROPERTY = "activeobjects.servicefactory.config.timeout";

    private static final long CONFIGURATION_TIMEOUT_MS = Integer.getInteger(CONFIGURATION_TIMEOUT_MS_PROPERTY, 180000);

    private static final String ENTITY_DEFAULT_PACKAGE = "ao.model";

    private final Bundle bundle;
    private final TenantProvider tenantProvider;
    private final ScheduledExecutorService configExecutor;

    @VisibleForTesting
    final AtomicReference<SettableFuture<ActiveObjectsConfiguration>> aoConfigFutureRef = new AtomicReference<SettableFuture<ActiveObjectsConfiguration>>(SettableFuture.<ActiveObjectsConfiguration>create());

    @VisibleForTesting
    final Runnable configCheckRunnable;

    @VisibleForTesting
    final LoadingCache<Tenant, Promise<ActiveObjects>> aoPromisesByTenant;

    TenantAwareActiveObjectsDelegate(
            @Nonnull final Bundle bundle,
            @Nonnull final ActiveObjectsFactory factory,
            @Nonnull final TenantProvider tenantProvider,
            @Nonnull final AOConfigurationGenerator aoConfigurationGenerator,
            @Nonnull final Function<Tenant, ExecutorService> initExecutorFunction,
            @Nonnull final ScheduledExecutorService configExecutor)
    {
        this.bundle = checkNotNull(bundle);
        this.tenantProvider = checkNotNull(tenantProvider);
        this.configExecutor = checkNotNull(configExecutor);
        checkNotNull(factory);
        checkNotNull(aoConfigurationGenerator);
        checkNotNull(initExecutorFunction);

        // loading cache for delegate promises by tenant
        aoPromisesByTenant = CacheBuilder.newBuilder().build(new CacheLoader<Tenant, Promise<ActiveObjects>>()
        {
            @Override
            public Promise<ActiveObjects> load(@Nonnull final Tenant tenant) throws Exception
            {
                logger.debug("bundle [{}] loading new AO promise for {}", bundle.getSymbolicName(), tenant);

                return Promises.forFuture(aoConfigFutureRef.get()).flatMap(new Function<ActiveObjectsConfiguration, Promise<ActiveObjects>>()
                {
                    @Override
                    public Promise<ActiveObjects> apply(@Nullable final ActiveObjectsConfiguration aoConfig)
                    {
                        logger.debug("bundle [{}] got ActiveObjectsConfiguration", bundle.getSymbolicName(), tenant);

                        return Promises.forFuture(initExecutorFunction.apply(tenant).submit(new Callable<ActiveObjects>()
                        {
                            @Override
                            public ActiveObjects call() throws Exception
                            {
                                logger.debug("bundle [{}] creating ActiveObjects", bundle.getSymbolicName());
                                try
                                {
                                    return factory.create(aoConfig, tenant);
                                }
                                catch (Exception e)
                                {
                                    throw new ActiveObjectsInitException("bundle [" + bundle.getSymbolicName() + "]", e);
                                }
                            }
                        }));
                    }
                });
            }
        });

        // warns if no aoConfigFutureRef set then attempts to generate one
        configCheckRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                if (!aoConfigFutureRef.get().isDone())
                {
                    logger.warn("bundle [{}] hasn't found an active objects configuration after {}ms; scanning default package '{}' for entities; note that this delay is configurable via the system property '{}'",
                            new Object[] { bundle.getSymbolicName(), CONFIGURATION_TIMEOUT_MS, ENTITY_DEFAULT_PACKAGE, CONFIGURATION_TIMEOUT_MS_PROPERTY });

                    ActiveObjectsConfiguration configuration = aoConfigurationGenerator.generateScannedConfiguration(bundle, ENTITY_DEFAULT_PACKAGE);
                    if (configuration != null)
                    {
                        aoConfigFutureRef.get().set(configuration);
                    }
                    else
                    {
                        RuntimeException e = new IllegalStateException("bundle [" + bundle.getSymbolicName() + "] has no active objects configuration - define an <ao> module descriptor");
                        aoConfigFutureRef.get().setException(e);
                    }
                }
            }
        };
    }

    public void init() throws InvalidSyntaxException
    {
        // listen to service registrations for ActiveObjectsConfiguration from this bundle only
        final String configFilter = "(&(objectclass=" + ActiveObjectsConfiguration.class.getName() + ")(com.atlassian.plugin.key=" + bundle.getSymbolicName() + "))";
        bundle.getBundleContext().addServiceListener(this, configFilter);
        logger.debug("bundle [{}] listening for {}ms for <ao> configuration service with filter {}; note that this period is configurable via the system property '{}'", new Object[] { bundle.getSymbolicName(), CONFIGURATION_TIMEOUT_MS_PROPERTY, configFilter, CONFIGURATION_TIMEOUT_MS_PROPERTY });

        // check that we actually receive the above registration
        configExecutor.schedule(configCheckRunnable, CONFIGURATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // attempt to get the configuration service now - it may already have been registered
        ServiceReference[] serviceReferences = bundle.getBundleContext().getServiceReferences(ActiveObjectsConfiguration.class.getName(), configFilter);
        if (serviceReferences != null)
        {
            if (serviceReferences.length == 1)
            {
                // got the one and only
                logger.debug("bundle [{}] init registered existing ActiveObjectsConfiguration", bundle.getSymbolicName());
                Object aoConfig = bundle.getBundleContext().getService(serviceReferences[0]);
                aoConfigFutureRef.get().set((ActiveObjectsConfiguration) aoConfig);
            }
            else if (serviceReferences.length > 1)
            {
                // multiple configurations registered already...
                throwMultipleAoConfigurationsException();
            }
        }

        // start things up now if we have a tenant
        Tenant tenant = tenantProvider.getTenant();
        if (tenant != null)
        {
            startActiveObjects(tenant);
        }
    }

    @Override
    public synchronized void serviceChanged(final ServiceEvent event)
    {
        switch (event.getType())
        {
            case ServiceEvent.REGISTERED:
            {
                Object registeredService = bundle.getBundleContext().getService(event.getServiceReference());
                if (aoConfigFutureRef.get().isDone())
                {
                    if (registeredService != getAoConfig())
                    {
                        // bad case - a different configuration has been registered
                        throwMultipleAoConfigurationsException();
                    }
                }
                else
                {
                    // good case - one configuration registered
                    aoConfigFutureRef.get().set((ActiveObjectsConfiguration) registeredService);
                    logger.debug("bundle [{}] registered service ActiveObjectsConfiguration", bundle.getSymbolicName());
                }
                break;
            }

            case ServiceEvent.UNREGISTERING:
            {
                // dutifully unregister
                bundle.getBundleContext().ungetService(event.getServiceReference());
                logger.debug("bundle [{}] unregistered service ActiveObjectsConfiguration", bundle.getSymbolicName());

                // have a new future throw an exception on resolution
                SettableFuture<ActiveObjectsConfiguration> exceptionalAoConfigFuture = SettableFuture.create();
                exceptionalAoConfigFuture.setException(new IllegalStateException("bundle [" + bundle.getSymbolicName() + "] has had its configuration service unregistered"));
                aoConfigFutureRef.set(exceptionalAoConfigFuture);

                break;
            }
        }
    }

    /**
     * the plugin has multiple <ao> configurations defined; blow up here and cause any future calls to the config to blow up
     */
    void throwMultipleAoConfigurationsException()
    {
        RuntimeException e = new IllegalStateException("bundle [" + bundle.getSymbolicName() + "] has multiple active objects configurations - only one active objects module descriptor <ao> allowed per plugin!");

        SettableFuture<ActiveObjectsConfiguration> exceptionalAoConfigFuture = SettableFuture.create();
        exceptionalAoConfigFuture.setException(e);
        aoConfigFutureRef.set(exceptionalAoConfigFuture);

        throw e;
    }

    /**
     * Danger! Danger Will Robinson!
     *
     * Convenience method purely to make calling code more readable.
     *
     * Pulls the value out of the future. It will block if the future isn't done.
     *
     * @throws java.lang.RuntimeException at the drop of a hat
     */
    private ActiveObjectsConfiguration getAoConfig()
    {
        try
        {
            return aoConfigFutureRef.get().get();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException(e);
        }
    }

    void startActiveObjects(@Nonnull final Tenant tenant)
    {
        checkNotNull(tenant);
        aoPromisesByTenant.getUnchecked(tenant);
    }

    void restartActiveObjects(@Nonnull final Tenant tenant)
    {
        checkNotNull(tenant);
        aoPromisesByTenant.invalidate(tenant);
        aoPromisesByTenant.getUnchecked(tenant);
    }

    @VisibleForTesting
    protected Promise<ActiveObjects> delegate()
    {
        Tenant tenant = tenantProvider.getTenant();
        if (tenant != null)
        {
            return aoPromisesByTenant.getUnchecked(tenant);
        }
        else
        {
            throw new NoDataSourceException();
        }
    }

    @Override
    public ActiveObjectsModuleMetaData moduleMetaData()
    {
        return new ActiveObjectsModuleMetaData()
        {
            @Override
            public void awaitInitialization() throws ExecutionException, InterruptedException
            {
                delegate().get();
            }

            @Override
            public void awaitInitialization(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException
            {
                delegate().get(timeout, unit);
            }

            @Override
            public boolean isInitialized()
            {
                Tenant tenant = tenantProvider.getTenant();
                if (tenant != null)
                {
                    Promise<ActiveObjects> aoPromise = aoPromisesByTenant.getUnchecked(tenant);
                    if (aoPromise.isDone())
                    {
                        try
                        {
                            aoPromise.claim();
                            return true;
                        }
                        catch (Exception e)
                        {
                            // any exception indicates a failure in initialisation, or at least that the delegate is not usable
                        }
                    }
                }
                return false;
            }

            @Override
            public DatabaseType getDatabaseType()
            {
                return delegate().claim().moduleMetaData().getDatabaseType();
            }

            @Override
            public boolean isDataSourcePresent()
            {
                return tenantProvider.getTenant() != null;
            }
        };
    }

    @Override
    public void migrate(final Class<? extends RawEntity<?>>... entities)
    {
        delegate().claim().migrate(entities);
    }

    @Override
    public void migrateDestructively(final Class<? extends RawEntity<?>>... entities)
    {
        delegate().claim().migrateDestructively(entities);
    }

    @Override
    public void flushAll()
    {
        delegate().claim().flushAll();
    }

    @Override
    public void flush(final RawEntity<?>... entities)
    {
        delegate().claim().flush(entities);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] get(final Class<T> type, final K... keys)
    {
        return delegate().claim().get(type, keys);
    }

    @Override
    public <T extends RawEntity<K>, K> T get(final Class<T> type, final K key)
    {
        return delegate().claim().get(type, key);
    }

    @Override
    public <T extends RawEntity<K>, K> T create(final Class<T> type, final DBParam... params)
    {
        return delegate().claim().create(type, params);
    }

    @Override
    public <T extends RawEntity<K>, K> T create(final Class<T> type, final Map<String, Object> params)
    {
        return delegate().claim().create(type, params);
    }

    @Override
    public void delete(final RawEntity<?>... entities)
    {
        delegate().claim().delete(entities);
    }

    @Override
    public <K> int deleteWithSQL(final Class<? extends RawEntity<K>> type, final String criteria, final Object... parameters)
    {
        return delegate().claim().deleteWithSQL(type, criteria, parameters);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] find(final Class<T> type)
    {
        return delegate().claim().find(type);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] find(final Class<T> type, final String criteria, final Object... parameters)
    {
        return delegate().claim().find(type, criteria, parameters);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] find(final Class<T> type, final Query query)
    {
        return delegate().claim().find(type, query);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] find(final Class<T> type, final String field, final Query query)
    {
        return delegate().claim().find(type, field, query);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] findWithSQL(final Class<T> type, final String keyField, final String sql, final Object... parameters)
    {
        return delegate().claim().findWithSQL(type, keyField, sql, parameters);
    }

    @Override
    public <T extends RawEntity<K>, K> void stream(final Class<T> type, final EntityStreamCallback<T, K> streamCallback)
    {
        delegate().claim().stream(type, streamCallback);
    }

    @Override
    public <T extends RawEntity<K>, K> void stream(final Class<T> type, final Query query, final EntityStreamCallback<T, K> streamCallback)
    {
        delegate().claim().stream(type, query, streamCallback);
    }

    @Override
    public <K> int count(final Class<? extends RawEntity<K>> type)
    {
        return delegate().claim().count(type);
    }

    @Override
    public <K> int count(final Class<? extends RawEntity<K>> type, final String criteria, final Object... parameters)
    {
        return delegate().claim().count(type, criteria, parameters);
    }

    @Override
    public <K> int count(final Class<? extends RawEntity<K>> type, final Query query)
    {
        return delegate().claim().count(type, query);
    }

    @Override
    public <T> T executeInTransaction(final TransactionCallback<T> callback)
    {
        return delegate().claim().executeInTransaction(callback);
    }

    public Bundle getBundle()
    {
        return bundle;
    }
}
