package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import net.java.ao.DBParam;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import java.io.Serializable;
import java.util.Map;

import static com.google.common.base.Preconditions.*;

/**
 * <p>This is a delegating ActiveObjects that will request the delegate from the given {@link Supplier}</p>
 */
final class DelegatingActiveObjects implements ActiveObjects
{
    private final MemoizingSupplier activeObjectsSupplier;

    public DelegatingActiveObjects(Supplier<ActiveObjects> activeObjectsSupplier)
    {
        this.activeObjectsSupplier = new MemoizingSupplier(checkNotNull(activeObjectsSupplier));
    }

    public void migrate(Class<? extends RawEntity<?>>... entities)
    {
        activeObjectsSupplier.get().migrate(entities);
    }

    public void flushAll()
    {
        activeObjectsSupplier.get().flushAll();
    }

    public void flush(RawEntity<?>... entities)
    {
        activeObjectsSupplier.get().flush(entities);
    }

    public <T extends RawEntity<K>, K> T[] get(Class<T> type, K... keys)
    {
        return activeObjectsSupplier.get().get(type, keys);
    }

    public <T extends RawEntity<K>, K> T get(Class<T> type, K key)
    {
        return activeObjectsSupplier.get().get(type, key);
    }

    public <T extends RawEntity<K>, K> T create(Class<T> type, DBParam... params)
    {
        return activeObjectsSupplier.get().create(type, params);
    }

    public <T extends RawEntity<K>, K> T create(Class<T> type, Map<String, Object> params)
    {
        return activeObjectsSupplier.get().create(type, params);
    }

    public void delete(RawEntity<?>... entities)
    {
        activeObjectsSupplier.get().delete(entities);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type)
    {
        return activeObjectsSupplier.get().find(type);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String criteria, Object... parameters)
    {
        return activeObjectsSupplier.get().find(type, criteria, parameters);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, Query query)
    {
        return activeObjectsSupplier.get().find(type, query);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String field, Query query)
    {
        return activeObjectsSupplier.get().find(type, field, query);
    }

    public <T extends RawEntity<K>, K> T[] findWithSQL(Class<T> type, String keyField, String sql, Object... parameters)
    {
        return activeObjectsSupplier.get().findWithSQL(type, keyField, sql, parameters);
    }

    public <T extends RawEntity<K>, K> void stream(Class<T> type, Query query, EntityStreamCallback<T, K> streamCallback)
    {
        activeObjectsSupplier.get().stream(type, query, streamCallback);
    }

    public <T extends RawEntity<K>, K> void stream(Class<T> type, EntityStreamCallback<T, K> streamCallback)
    {
        activeObjectsSupplier.get().stream(type, streamCallback);
    }

    public <K> int count(Class<? extends RawEntity<K>> type)
    {
        return activeObjectsSupplier.get().count(type);
    }

    public <K> int count(Class<? extends RawEntity<K>> type, String criteria, Object... parameters)
    {
        return activeObjectsSupplier.get().count(type, criteria, parameters);
    }

    public <K> int count(Class<? extends RawEntity<K>> type, Query query)
    {
        return activeObjectsSupplier.get().count(type, query);
    }

    public <T> T executeInTransaction(TransactionCallback<T> callback)
    {
        return activeObjectsSupplier.get().executeInTransaction(callback);
    }

    public void removeDelegate()
    {
        activeObjectsSupplier.remove();
    }

    private static final class MemoizingSupplier implements Supplier<ActiveObjects>, Serializable
    {
        final Supplier<ActiveObjects> delegate;
        transient boolean initialized;
        transient ActiveObjects value;

        MemoizingSupplier(Supplier<ActiveObjects> delegate)
        {
            this.delegate = delegate;
        }

        public synchronized ActiveObjects get()
        {
            if (!initialized)
            {
                value = delegate.get();
                initialized = true;
            }
            return value;
        }

        public synchronized void remove()
        {
            if (initialized)
            {
                value.flushAll();
                value = null;
                initialized = false;
            }
        }

        private static final long serialVersionUID = 0;
    }
}
