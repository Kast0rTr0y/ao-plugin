package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsModuleMetaData;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.activeobjects.util.ActiveObjectsConfigurationServiceProvider;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;
import com.google.common.base.Supplier;
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
    private final Supplier<ActiveObjectsServiceFactory.DataSource> dataSourceSupplier;
    private final Supplier<ExecutorService> initExecutorSupplier;

    BabyBearActiveObjectsDelegate(final Bundle bundle,
            final ActiveObjectsFactory factory,
            final ActiveObjectsConfigurationServiceProvider aoConfigurationResolver,
            final DataSourceProvider dataSourceProvider,
            final TransactionTemplate transactionTemplate,
            final Supplier<ActiveObjectsServiceFactory.DataSource> dataSourceSupplier,
            final Supplier<ExecutorService> initExecutorSupplier)
    {
        this.bundle = checkNotNull(bundle);
        this.factory = checkNotNull(factory);
        this.aoConfigurationResolver = checkNotNull(aoConfigurationResolver);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.transactionTemplate = checkNotNull(transactionTemplate);
        this.dataSourceSupplier = checkNotNull(dataSourceSupplier);
        this.initExecutorSupplier = checkNotNull(initExecutorSupplier);
    }

    private final LoadingCache<ActiveObjectsServiceFactory.DataSource, Promise<ActiveObjects>> promisedActiveObjectses = CacheBuilder.newBuilder().build(new CacheLoader<ActiveObjectsServiceFactory.DataSource, Promise<ActiveObjects>>()
    {
        @Override
        public Promise<ActiveObjects> load(final ActiveObjectsServiceFactory.DataSource key) throws Exception
        {
            logger.debug("bundle [{}] loading new AO promise for {}", bundle.getSymbolicName(), key);

            return Promises.forFuture(initExecutorSupplier.get().submit(new Callable<ActiveObjects>()
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

                    return factory.create(configuration, databaseType);
                }
            }));
        }
    });

    void startActiveObjects(ActiveObjectsServiceFactory.DataSource dataSource)
    {
        promisedActiveObjectses.getUnchecked(dataSource);
    }

    void restartActiveObjects(ActiveObjectsServiceFactory.DataSource dataSource)
    {
        promisedActiveObjectses.invalidate(dataSource);
        startActiveObjects(dataSource);
    }

    private ActiveObjects delegate()
    {
        ActiveObjectsServiceFactory.DataSource dataSource = dataSourceSupplier.get();
        if (dataSource != null)
        {
            // wait for the promise which may have just started
            return promisedActiveObjectses.getUnchecked(dataSource).claim();
        }
        else
        {
            throw new IllegalStateException("Baby Bear has no database... be patient...");
        }
    }

    @Override
    public ActiveObjectsModuleMetaData moduleMetaData()
    {
        return new ActiveObjectsModuleMetaData()
        {
            /**
             * Block until promisedAORef is fulfilled
             *
             * @throws com.atlassian.activeobjects.internal.ActiveObjectsInitException on issues during the execution of the promise
             */
            @Override
            public void awaitInitialization()
            {
                logger.warn("ActiveObjectsModuleMetaData is deprecated; doing nothing");
            }

            /**
             * Immediately return the state
             *
             * @return true if promisedAORef has been fulfilled
             */
            @Override
            public boolean isInitialized()
            {
                logger.warn("ActiveObjectsModuleMetaData is deprecated; returning true");
                return true;
            }

            /**
             * Database type that AO was last initialised with
             *
             * @return possibly UNKNOWN
             */
            @Override
            public DatabaseType getDatabaseType()
            {
                logger.warn("ActiveObjectsModuleMetaData is deprecated; returning DatabaseType.UNKNOWN");
                return DatabaseType.UNKNOWN;
            }
        };
    }

    @Override
    public void migrate(final Class<? extends RawEntity<?>>... entities)
    {
        delegate().migrate(entities);
    }

    @Override
    public void migrateDestructively(final Class<? extends RawEntity<?>>... entities)
    {
        delegate().migrateDestructively(entities);
    }

    @Override
    public void flushAll()
    {
        delegate().flushAll();
    }

    @Override
    public void flush(final RawEntity<?>... entities)
    {
        delegate().flush(entities);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] get(final Class<T> type, final K... keys)
    {
        return delegate().get(type, keys);
    }

    @Override
    public <T extends RawEntity<K>, K> T get(final Class<T> type, final K key)
    {
        return delegate().get(type, key);
    }

    @Override
    public <T extends RawEntity<K>, K> T create(final Class<T> type, final DBParam... params)
    {
        return delegate().create(type, params);
    }

    @Override
    public <T extends RawEntity<K>, K> T create(final Class<T> type, final Map<String, Object> params)
    {
        return delegate().create(type, params);
    }

    @Override
    public void delete(final RawEntity<?>... entities)
    {
        delegate().delete(entities);
    }

    @Override
    public <K> int deleteWithSQL(final Class<? extends RawEntity<K>> type, final String criteria, final Object... parameters)
    {
        return delegate().deleteWithSQL(type, criteria, parameters);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] find(final Class<T> type)
    {
        return delegate().find(type);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] find(final Class<T> type, final String criteria, final Object... parameters)
    {
        return delegate().find(type, criteria, parameters);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] find(final Class<T> type, final Query query)
    {
        return delegate().find(type, query);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] find(final Class<T> type, final String field, final Query query)
    {
        return delegate().find(type, field, query);
    }

    @Override
    public <T extends RawEntity<K>, K> T[] findWithSQL(final Class<T> type, final String keyField, final String sql, final Object... parameters)
    {
        return delegate().findWithSQL(type, keyField, sql, parameters);
    }

    @Override
    public <T extends RawEntity<K>, K> void stream(final Class<T> type, final EntityStreamCallback<T, K> streamCallback)
    {
        delegate().stream(type, streamCallback);
    }

    @Override
    public <T extends RawEntity<K>, K> void stream(final Class<T> type, final Query query, final EntityStreamCallback<T, K> streamCallback)
    {
        delegate().stream(type, query, streamCallback);
    }

    @Override
    public <K> int count(final Class<? extends RawEntity<K>> type)
    {
        return delegate().count(type);
    }

    @Override
    public <K> int count(final Class<? extends RawEntity<K>> type, final String criteria, final Object... parameters)
    {
        return delegate().count(type, criteria, parameters);
    }

    @Override
    public <K> int count(final Class<? extends RawEntity<K>> type, final Query query)
    {
        return delegate().count(type, query);
    }

    @Override
    public <T> T executeInTransaction(final TransactionCallback<T> callback)
    {
        return delegate().executeInTransaction(callback);
    }

    public Bundle getBundle()
    {
        return bundle;
    }
}
