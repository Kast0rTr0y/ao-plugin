package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.TransactionCallback;
import com.atlassian.activeobjects.external.TransactionStatus;
import net.java.ao.DBParam;
import net.java.ao.EntityManager;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import net.java.ao.Transaction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * <p>Implementation of {@link com.atlassian.activeobjects.external.ActiveObjects} that mainly delegates to the
 * {@link net.java.ao.EntityManager}.</p>
 * <p>This is {@code abstract} and concrete implementations should have to provide a correctly configured {@link net.java.ao.EntityManager}</p>
 *
 * @see net.java.ao.EntityManager
 */
class EntityManagedActiveObjects implements ActiveObjects
{
    private final EntityManager entityManager;

    private final Collection<Class<? extends RawEntity<?>>> entities;

    protected EntityManagedActiveObjects(EntityManager entityManager)
    {
        this.entityManager = checkNotNull(entityManager);
        this.entities = new HashSet<Class<? extends RawEntity<?>>>();
    }

    ///CLOVER:OFF

    public final void migrate(Class<? extends RawEntity<?>>... entities) throws SQLException
    {
        entityManager.migrate(entities);
        this.entities.addAll(Arrays.asList(entities));
    }

    public InputStream backup()
    {
//        return entityManager.backup(entities.toArray(new Class[0]));
        return new ByteArrayInputStream(new byte[0]);
    }

    public void restore(InputStream backup)
    {
//        entityManager.restore(backup);
    }

    public final void flushAll()
    {
        entityManager.flushAll();
    }

    public final void flush(RawEntity<?>... entities)
    {
        entityManager.flush(entities);
    }

    public final <T extends RawEntity<K>, K> T[] get(Class<T> type, K... keys)
    {
        return entityManager.get(type, keys);
    }

    public final <T extends RawEntity<K>, K> T get(Class<T> type, K key)
    {
        return entityManager.get(type, key);
    }

    public final <T extends RawEntity<K>, K> T create(Class<T> type, DBParam... params) throws SQLException
    {
        return entityManager.create(type, params);
    }

    public final <T extends RawEntity<K>, K> T create(Class<T> type, Map<String, Object> params) throws SQLException
    {
        return entityManager.create(type, params);
    }

    public final void delete(RawEntity<?>... entities) throws SQLException
    {
        entityManager.delete(entities);
    }

    public final <T extends RawEntity<K>, K> T[] find(Class<T> type) throws SQLException
    {
        return entityManager.find(type);
    }

    public final <T extends RawEntity<K>, K> T[] find(Class<T> type, String criteria, Object... parameters) throws SQLException
    {
        return entityManager.find(type, criteria, parameters);
    }

    public final <T extends RawEntity<K>, K> T[] find(Class<T> type, Query query) throws SQLException
    {
        return entityManager.find(type, query);
    }

    public final <T extends RawEntity<K>, K> T[] find(Class<T> type, String field, Query query) throws SQLException
    {
        return entityManager.find(type, field, query);
    }

    public final <T extends RawEntity<K>, K> T[] findWithSQL(Class<T> type, String keyField, String sql, Object... parameters) throws SQLException
    {
        return entityManager.findWithSQL(type, keyField, sql, parameters);
    }

    public final <K> int count(Class<? extends RawEntity<K>> type) throws SQLException
    {
        return entityManager.count(type);
    }

    public final <K> int count(Class<? extends RawEntity<K>> type, String criteria, Object... parameters) throws SQLException
    {
        return entityManager.count(type, criteria, parameters);
    }

    public final <K> int count(Class<? extends RawEntity<K>> type, Query query) throws SQLException
    {
        return entityManager.count(type, query);
    }

    ///CLOVER:ON

    public final <T> T executeInTransaction(final TransactionCallback<T> callback) throws SQLException
    {
        return new Transaction<T>(entityManager)
        {
            @Override
            public T run() throws SQLException
            {
                return callback.doInTransaction(new TransactionStatus()
                {
                    public Connection getConnection()
                    {
                        try
                        {
                            return entityManager.getProvider().getConnection();
                        }
                        catch (SQLException e)
                        {
                            throw new ActiveObjectsPluginException(e);
                        }
                    }
                });
            }
        }.execute();
    }
}
