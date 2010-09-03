package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsProvider;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.DBParam;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

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

    public void migrate(Class<? extends RawEntity<?>>... entities) throws SQLException
    {
        getDelegate().migrate(entities);
    }

    public InputStream backup()
    {
        return getDelegate().backup();
    }

    public void restore(InputStream is)
    {
        getDelegate().restore(is);
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

    public <T extends RawEntity<K>, K> T create(Class<T> type, DBParam... params) throws SQLException
    {
        return getDelegate().create(type, params);
    }

    public <T extends RawEntity<K>, K> T create(Class<T> type, Map<String, Object> params) throws SQLException
    {
        return getDelegate().create(type, params);
    }

    public void delete(RawEntity<?>... entities) throws SQLException
    {
        getDelegate().delete(entities);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type) throws SQLException
    {
        return getDelegate().find(type);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String criteria, Object... parameters) throws SQLException
    {
        return getDelegate().find(type, criteria, parameters);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, Query query) throws SQLException
    {
        return getDelegate().find(type, query);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String field, Query query) throws SQLException
    {
        return getDelegate().find(type, field, query);
    }

    public <T extends RawEntity<K>, K> T[] findWithSQL(Class<T> type, String keyField, String sql, Object... parameters) throws SQLException
    {
        return getDelegate().findWithSQL(type, keyField, sql, parameters);
    }

    public <K> int count(Class<? extends RawEntity<K>> type) throws SQLException
    {
        return getDelegate().count(type);
    }

    public <K> int count(Class<? extends RawEntity<K>> type, String criteria, Object... parameters) throws SQLException
    {
        return getDelegate().count(type, criteria, parameters);
    }

    public <K> int count(Class<? extends RawEntity<K>> type, Query query) throws SQLException
    {
        return getDelegate().count(type, query);
    }

    public <T> T executeInTransaction(TransactionCallback<T> callback) throws SQLException
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
