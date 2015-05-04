package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsModuleMetaData;
import com.atlassian.activeobjects.external.NoDataSourceException;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.ActiveObjectsInitException;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.tenancy.api.TenantContext;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
class TenantAwareActiveObjects implements ActiveObjects
{
    private static final Logger logger = LoggerFactory.getLogger(TenantAwareActiveObjects.class);

    private final Bundle bundle;
    private final TenantContext tenantContext;

    @VisibleForTesting
    final SettableFuture<ActiveObjectsConfiguration> aoConfigFuture = SettableFuture.create();

    @VisibleForTesting
    final LoadingCache<Tenant, Promise<ActiveObjects>> aoPromisesByTenant;

    TenantAwareActiveObjects(
            @Nonnull final Bundle bundle,
            @Nonnull final ActiveObjectsFactory factory,
            @Nonnull final TenantContext tenantContext,
            @Nonnull final Function<Tenant, ExecutorService> initExecutorFunction)
    {
        this.bundle = checkNotNull(bundle);
        this.tenantContext = checkNotNull(tenantContext);
        checkNotNull(factory);
        checkNotNull(initExecutorFunction);

        // loading cache for delegate promises by tenant
        aoPromisesByTenant = CacheBuilder.newBuilder().build(new CacheLoader<Tenant, Promise<ActiveObjects>>()
        {
            @Override
            public Promise<ActiveObjects> load(@Nonnull final Tenant tenant) throws Exception
            {
                logger.debug("bundle [{}] loading new AO promise for {}", bundle.getSymbolicName(), tenant);

                return Promises.forFuture(aoConfigFuture).flatMap(new Function<ActiveObjectsConfiguration, Promise<ActiveObjects>>()
                {
                    @Override
                    public Promise<ActiveObjects> apply(@Nullable final ActiveObjectsConfiguration aoConfig)
                    {
                        logger.debug("bundle [{}] got ActiveObjectsConfiguration", bundle.getSymbolicName(), tenant);

                        final SettableFuture<ActiveObjects> aoFuture = SettableFuture.create();
                        //noinspection ConstantConditions
                        initExecutorFunction.apply(tenant).submit(new Callable<Void>()
                        {
                            @Override
                            public Void call() throws Exception
                            {
                                logger.debug("bundle [{}] creating ActiveObjects", bundle.getSymbolicName());
                                try
                                {
                                    final ActiveObjects ao = factory.create(aoConfig, tenant);
                                    logger.debug("bundle [{}] created ActiveObjects", bundle.getSymbolicName());
                                    aoFuture.set(ao);
                                }
                                catch (Exception e)
                                {
                                    final ActiveObjectsInitException activeObjectsInitException = new ActiveObjectsInitException("bundle [" + bundle.getSymbolicName() + "]", e);
                                    aoFuture.setException(activeObjectsInitException);
                                }
                                return null;
                            }
                        });

                        return Promises.forFuture(aoFuture);
                    }
                });
            }
        });
    }

    public void init()
    {
        logger.debug("bundle [{}] init", bundle.getSymbolicName());

        // start things up now if we have a tenant
        Tenant tenant = tenantContext.getCurrentTenant();
        if (tenant != null)
        {
            aoPromisesByTenant.invalidate(tenant);
            startActiveObjects(tenant);
        }
    }

    public void destroy()
    {
        aoConfigFuture.cancel(false);
        for (Promise<ActiveObjects> aoPromise : aoPromisesByTenant.asMap().values())
        {
            aoPromise.cancel(false);
        }
    }

    void setAoConfiguration(@Nonnull final ActiveObjectsConfiguration aoConfiguration)
    {
        logger.debug("setAoConfiguration [{}]", bundle.getSymbolicName());

        if (aoConfigFuture.isDone())
        {
            final RuntimeException e = new IllegalStateException("bundle [" + bundle.getSymbolicName() + "] has multiple active objects configurations - only one active objects module descriptor <ao> allowed per plugin!");
            aoConfigFuture.setException(e);
            throw e;
        }
        else
        {
            aoConfigFuture.set(aoConfiguration);
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
        if (!aoConfigFuture.isDone())
        {
            throw new IllegalStateException("plugin [{" + bundle.getSymbolicName() + "}] invoking ActiveObjects before <ao> configuration module is enabled or plugin is missing an <ao> configuration module. Note that scanning of entities from the ao.model package is no longer supported.");
        }

        Tenant tenant = tenantContext.getCurrentTenant();
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
                Tenant tenant = tenantContext.getCurrentTenant();
                if (tenant != null)
                {
                    aoPromisesByTenant.getUnchecked(tenant).get();
                }
                else
                {
                    throw new NoDataSourceException();
                }
            }

            @Override
            public void awaitInitialization(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException
            {
                Tenant tenant = tenantContext.getCurrentTenant();
                if (tenant != null)
                {
                    aoPromisesByTenant.getUnchecked(tenant).get(timeout, unit);
                }
                else
                {
                    throw new NoDataSourceException();
                }
            }

            @Override
            public boolean isInitialized()
            {
                Tenant tenant = tenantContext.getCurrentTenant();
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
                return tenantContext.getCurrentTenant() != null;
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
