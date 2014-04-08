package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsModuleMetaData;
import com.atlassian.activeobjects.external.NoDataSourceException;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.TenantProvider;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.activeobjects.util.ActiveObjectsConfigurationServiceProvider;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.util.concurrent.Function;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.java.ao.DBParam;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 */
final class BabyBearActiveObjectsDelegate implements ActiveObjects
{
    private static final Logger logger = LoggerFactory.getLogger(BabyBearActiveObjectsDelegate.class);

    private final Bundle bundle;
    private final ActiveObjectsFactory factory;
    private final ActiveObjectsConfigurationServiceProvider aoConfigurationResolver;
    private final DataSourceProvider dataSourceProvider;
    private final TransactionTemplate transactionTemplate;
    private final TenantProvider tenantProvider;
    private final Function<Tenant, ExecutorService> initExecutorFunction;

    BabyBearActiveObjectsDelegate(@Nonnull final Bundle bundle,
            @Nonnull final ActiveObjectsFactory factory,
            @Nonnull final ActiveObjectsConfigurationServiceProvider aoConfigurationResolver,
            @Nonnull final DataSourceProvider dataSourceProvider,
            @Nonnull final TransactionTemplate transactionTemplate,
            @Nonnull final TenantProvider tenantProvider,
            @Nonnull final Function<Tenant, ExecutorService> initExecutorFunction)
    {
        this.bundle = checkNotNull(bundle);
        this.factory = checkNotNull(factory);
        this.aoConfigurationResolver = checkNotNull(aoConfigurationResolver);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.transactionTemplate = checkNotNull(transactionTemplate);
        this.tenantProvider = checkNotNull(tenantProvider);
        this.initExecutorFunction = checkNotNull(initExecutorFunction);

        // start things up now if we have a tenant
        Tenant tenant = tenantProvider.getTenant();
        if (tenant != null)
        {
            startActiveObjects(tenant);
        }
    }

    private final LoadingCache<Tenant, Promise<ActiveObjects>> aoPromises = CacheBuilder.newBuilder().build(new CacheLoader<Tenant, Promise<ActiveObjects>>()
    {
        @Override
        public Promise<ActiveObjects> load(final Tenant tenant) throws Exception
        {
            logger.debug("bundle [{}] loading new AO promise for {}", bundle.getSymbolicName(), tenant);

            return Promises.forFuture(initExecutorFunction.get(tenant).submit(new Callable<ActiveObjects>()
            {
                @Override
                public ActiveObjects call() throws Exception
                {
                    logger.debug("creating ActiveObjects for bundle [{}]", bundle.getSymbolicName());

                    // This is executed in a transaction as some providers create a hibernate session which can only be done in a transaction
                    DatabaseType databaseType = transactionTemplate.execute(new TransactionCallback<DatabaseType>()
                    {
                        @Override
                        public DatabaseType doInTransaction()
                        {
                            return checkNotNull(dataSourceProvider.getDatabaseType(), dataSourceProvider + " returned null for dbType");
                        }
                    });
                    logger.debug("retrieved databaseType={} for bundle [{}]", databaseType, bundle.getSymbolicName());

                    ActiveObjectsConfiguration configuration = aoConfigurationResolver.getAndWait(bundle);
                    logger.debug("retrieved AO configuration for bundle [{}]", bundle.getSymbolicName());

                    ActiveObjects activeObjects = factory.create(configuration, databaseType);
                    logger.debug("created AO for bundle [{}]", bundle.getSymbolicName());

                    return activeObjects;
                }
            }));
        }
    });

    void startActiveObjects(@Nonnull final Tenant tenant)
    {
        checkNotNull(tenant);
        aoPromises.getUnchecked(tenant);
    }

    void restartActiveObjects(@Nonnull final Tenant tenant)
    {
        checkNotNull(tenant);
        aoPromises.invalidate(tenant);
        aoPromises.getUnchecked(tenant);
    }

    private Promise<ActiveObjects> delegate()
    {
        Tenant tenant = tenantProvider.getTenant();
        if (tenant != null)
        {
            return aoPromises.getUnchecked(tenant);
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
            public void awaitInitialization()
            {
                delegate().claim();
            }

            @Override
            public boolean isInitialized()
            {
                return delegate().isDone();
            }

            @Override
            public DatabaseType getDatabaseType()
            {
                return delegate().claim().moduleMetaData().getDatabaseType();
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
