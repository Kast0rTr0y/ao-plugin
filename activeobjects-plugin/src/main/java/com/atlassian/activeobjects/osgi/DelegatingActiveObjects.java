package com.atlassian.activeobjects.osgi;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import net.java.ao.DBParam;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsProvider;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * <p>This is a delegating ActiveObjects that will request the delegate from the given {@link com.atlassian.activeobjects.internal.ActiveObjectsProvider}</p>
 */
final class DelegatingActiveObjects implements ActiveObjects
{
    private final ActiveObjectsConfiguration configuration;
    private final ActiveObjectsProvider provider;

    public DelegatingActiveObjects(ActiveObjectsConfiguration configuration, ActiveObjectsProvider provider)
    {
        this.configuration = checkNotNull(configuration);
        this.provider = checkNotNull(provider);
    }

    public void migrate(Class<? extends RawEntity<?>>... entities)
    {
        getDelegate().migrate(entities);
    }

    public void flushAll()
    {
        getDelegate().flushAll();
    }

    public void flush(RawEntity<?>... entities)
    {
        getDelegate().flush(entities);
    }

    public <T extends RawEntity<K>, K> T[] get(Class<T> type, K... keys)
    {
        return getDelegate().get(type, keys);
    }

    public <T extends RawEntity<K>, K> T get(Class<T> type, K key)
    {
        return getDelegate().get(type, key);
    }

    public <T extends RawEntity<K>, K> T create(Class<T> type, DBParam... params)
    {
        return getDelegate().create(type, params);
    }

    public <T extends RawEntity<K>, K> T create(Class<T> type, Map<String, Object> params)
    {
        return getDelegate().create(type, params);
    }

    public void delete(RawEntity<?>... entities)
    {
        getDelegate().delete(entities);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type)
    {
        return getDelegate().find(type);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String criteria, Object... parameters)
    {
        return getDelegate().find(type, criteria, parameters);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, Query query)
    {
        return getDelegate().find(type, query);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String field, Query query)
    {
        return getDelegate().find(type, field, query);
    }

    public <T extends RawEntity<K>, K> T[] findWithSQL(Class<T> type, String keyField, String sql, Object... parameters)
    {
        return getDelegate().findWithSQL(type, keyField, sql, parameters);
    }

    public <T extends RawEntity<K>, K> void stream(Class<T> type, Query query, EntityStreamCallback<T, K> streamCallback)
    {
        getDelegate().stream(type, query, streamCallback);
    }

    public <T extends RawEntity<K>, K> void stream(Class<T> type, EntityStreamCallback<T, K> streamCallback)
    {
        getDelegate().stream(type, streamCallback);
    }
    
    public <K> int count(Class<? extends RawEntity<K>> type)
    {
        return getDelegate().count(type);
    }

    public <K> int count(Class<? extends RawEntity<K>> type, String criteria, Object... parameters)
    {
        return getDelegate().count(type, criteria, parameters);
    }

    public <K> int count(Class<? extends RawEntity<K>> type, Query query)
    {
        return getDelegate().count(type, query);
    }

    public <T> T executeInTransaction(TransactionCallback<T> callback)
    {
        return getDelegate().executeInTransaction(callback);
    }

    ActiveObjectsConfiguration getConfiguration()
    {
        return configuration;
    }

    ActiveObjectsProvider getProvider()
    {
        return provider;
    }

    private ActiveObjects getDelegate()
    {
        return provider.get(configuration);
    }
}
