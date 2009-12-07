package com.atlassian.labs.activeobjects.internal;

import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.DBParam;
import net.java.ao.Query;
import net.java.ao.Transaction;
import com.atlassian.labs.activeobjects.external.ActiveObjects;
import com.atlassian.labs.activeobjects.external.TransactionCallback;

import java.sql.SQLException;
import java.util.Map;

/**
 * Simply delegates back to the underlying {@link EntityManager}
 */
public class DefaultActiveObjects implements ActiveObjects
{
    private final EntityManager entityManager;
    public DefaultActiveObjects(String uri, String username, String password)
    {
        entityManager = new EntityManager(uri, username, password);
    }

    public void migrate(Class<? extends RawEntity<?>>... entities) throws SQLException
    {
        entityManager.migrate(entities);
    }

    public void flushAll()
    {
        entityManager.flushAll();
    }

    public void flush(RawEntity<?>... entities)
    {
        entityManager.flush(entities);
    }

    public <T extends RawEntity<K>, K> T[] get(Class<T> type, K... keys)
    {
        return entityManager.get(type, keys);
    }

    public <T extends RawEntity<K>, K> T get(Class<T> type, K key)
    {
        return entityManager.get(type, key);
    }

    public <T extends RawEntity<K>, K> T create(Class<T> type, DBParam... params) throws SQLException
    {
        return entityManager.create(type, params);
    }

    public <T extends RawEntity<K>, K> T create(Class<T> type, Map<String, Object> params) throws SQLException
    {
        return entityManager.create(type, params);
    }

    public void delete(RawEntity<?>... entities) throws SQLException
    {
        entityManager.delete(entities);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type) throws SQLException
    {
        return entityManager.find(type);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String criteria, Object... parameters) throws SQLException
    {
        return entityManager.find(type, criteria, parameters);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, Query query) throws SQLException
    {
        return entityManager.find(type, query);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String field, Query query) throws SQLException
    {
        return entityManager.find(type, field, query);
    }

    public <T extends RawEntity<K>, K> T[] findWithSQL(Class<T> type, String keyField, String sql, Object... parameters) throws SQLException
    {
        return entityManager.findWithSQL(type, keyField, sql, parameters);
    }

    public <K> int count(Class<? extends RawEntity<K>> type) throws SQLException
    {
        return entityManager.count(type);
    }

    public <K> int count(Class<? extends RawEntity<K>> type, String criteria, Object... parameters) throws SQLException
    {
        return entityManager.count(type, criteria, parameters);
    }

    public <K> int count(Class<? extends RawEntity<K>> type, Query query) throws SQLException
    {
        return entityManager.count(type, query);
    }

    public <T> T executeInTransaction(final TransactionCallback<T> callback) throws SQLException
    {
        return new Transaction<T>(entityManager)
        {
            @Override
            public T run() throws SQLException
            {
                return callback.doInTransaction();
            }
        }.execute();
    }
}