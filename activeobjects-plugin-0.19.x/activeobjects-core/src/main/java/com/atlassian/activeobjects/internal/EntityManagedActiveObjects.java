package com.atlassian.activeobjects.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.SQLException;
import java.util.Map;

import net.java.ao.DBParam;
import net.java.ao.DefaultPolymorphicTypeMapper;
import net.java.ao.EntityManager;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * <p>Implementation of {@link com.atlassian.activeobjects.external.ActiveObjects} that mainly delegates to the
 * {@link net.java.ao.EntityManager}.</p>
 * <p>This is {@code abstract} and concrete implementations should have to provide a correctly configured {@link net.java.ao.EntityManager}</p>
 *
 * @see net.java.ao.EntityManager
 */
public class EntityManagedActiveObjects implements ActiveObjects
{
    private final EntityManager entityManager;
    private final TransactionManager transactionManager;

    protected EntityManagedActiveObjects(EntityManager entityManager, TransactionManager transactionManager)
    {
        this.entityManager = checkNotNull(entityManager);
        this.transactionManager = checkNotNull(transactionManager);
    }

    ///CLOVER:OFF

    public final void migrate(Class<? extends RawEntity<?>>... entities)
    {
        try
        {
            entityManager.setPolymorphicTypeMapper(new DefaultPolymorphicTypeMapper(entities));
            entityManager.migrate(entities);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
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
        try
        {
            return entityManager.get(type, keys);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final <T extends RawEntity<K>, K> T get(Class<T> type, K key)
    {
        try
        {
            return entityManager.get(type, key);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final <T extends RawEntity<K>, K> T create(Class<T> type, DBParam... params)
    {
        try
        {
            return entityManager.create(type, params);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final <T extends RawEntity<K>, K> T create(Class<T> type, Map<String, Object> params)
    {
        try
        {
            return entityManager.create(type, params);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final void delete(RawEntity<?>... entities)
    {
        try
        {
            entityManager.delete(entities);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final <T extends RawEntity<K>, K> T[] find(Class<T> type)
    {
        try
        {
            return entityManager.find(type);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final <T extends RawEntity<K>, K> T[] find(Class<T> type, String criteria, Object... parameters)
    {
        try
        {
            return entityManager.find(type, criteria, parameters);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final <T extends RawEntity<K>, K> T[] find(Class<T> type, Query query)
    {
        try
        {
            return entityManager.find(type, query);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final <T extends RawEntity<K>, K> T[] find(Class<T> type, String field, Query query)
    {
        try
        {
            return entityManager.find(type, field, query);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final <T extends RawEntity<K>, K> T[] findWithSQL(Class<T> type, String keyField, String sql, Object... parameters)
    {
        try
        {
            return entityManager.findWithSQL(type, keyField, sql, parameters);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }
    
    public final <T extends RawEntity<K>, K> void stream(Class<T> type, Query query, EntityStreamCallback<T, K> streamCallback)
    {
        try
        {
            entityManager.stream(type, query, streamCallback);
        } 
        catch (SQLException e) 
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final <T extends RawEntity<K>, K> void stream(Class<T> type, EntityStreamCallback<T, K> streamCallback)
    {
        try
        {
            entityManager.stream(type, streamCallback);
        } 
        catch (SQLException e) 
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }
    
    public final <K> int count(Class<? extends RawEntity<K>> type)
    {
        try
        {
            return entityManager.count(type);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final <K> int count(Class<? extends RawEntity<K>> type, String criteria, Object... parameters)
    {
        try
        {
            return entityManager.count(type, criteria, parameters);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    public final <K> int count(Class<? extends RawEntity<K>> type, Query query)
    {
        try
        {
            return entityManager.count(type, query);
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsSqlException(entityManager, e);
        }
    }

    ///CLOVER:ON
    public final <T> T executeInTransaction(final TransactionCallback<T> callback)
    {
        return transactionManager.doInTransaction(callback);
    }
}
