package com.atlassian.activeobjects.osgi;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsInitException;
import com.atlassian.activeobjects.spi.TransactionSynchronisationManager;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;
import com.google.common.base.Function;
import com.google.common.base.Supplier;

import net.java.ao.DBParam;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import org.osgi.framework.Bundle;

/**
 * <p>This is a delegating ActiveObjects that will request the delegate from the given {@link Supplier}</p>
 */
final class DelegatingActiveObjects implements ActiveObjects
{
    private final AtomicReference<Promise<ActiveObjects>> promisedAORef = new AtomicReference<Promise<ActiveObjects>>();
    
    private final Bundle bundle;
    
    private final TransactionSynchronisationManager tranSyncManager;
    
    public DelegatingActiveObjects(Promise<ActiveObjects> promise, Bundle bundle, TransactionSynchronisationManager tranSyncManager)
    {
        this.bundle = bundle;
        promisedAORef.set(promise);
        this.tranSyncManager = tranSyncManager;
    }

    private ActiveObjects getPromisedAO()
    {
        Promise<ActiveObjects> promise = promisedAORef.get();
        if(tranSyncManager.isActiveTransaction())
        {
            if(!promise.isDone())
                throw new ActiveObjectsInitException("ActiveObjects was called from within a transaction before initialization had complete, this can cause deadlocks.\n" +
                        "Not waiting for ActiveObjects to complete migration.  To avoid this error and wait for initialization to be complete, " +
                        "call ActiveObjects.awaitInitialization from outside of a transcation in response to a PluginEnabledEvent.");
        }
        return checkNotNull(promise.claim());
    }

    public void migrate(Class<? extends RawEntity<?>>... entities)
    {
        getPromisedAO().migrate(entities);
    }

    public void migrateDestructively(Class<? extends RawEntity<?>>... entities)
    {
        getPromisedAO().migrateDestructively(entities);
    }

    public void flushAll()
    {
        getPromisedAO().flushAll();
    }

    public void flush(RawEntity<?>... entities)
    {
        getPromisedAO().flush(entities);
    }

    public <T extends RawEntity<K>, K> T[] get(Class<T> type, K... keys)
    {
        return getPromisedAO().get(type, keys);
    }

    public <T extends RawEntity<K>, K> T get(Class<T> type, K key)
    {
        return getPromisedAO().get(type, key);
    }

    public <T extends RawEntity<K>, K> T create(Class<T> type, DBParam... params)
    {
        return getPromisedAO().create(type, params);
    }

    public <T extends RawEntity<K>, K> T create(Class<T> type, Map<String, Object> params)
    {
        return getPromisedAO().create(type, params);
    }

    public void delete(RawEntity<?>... entities)
    {
        getPromisedAO().delete(entities);
    }

    public <K> int deleteWithSQL(Class<? extends RawEntity<K>> type, String criteria, Object... parameters)
    {
        return getPromisedAO().deleteWithSQL(type, criteria, parameters);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type)
    {
        return getPromisedAO().find(type);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String criteria, Object... parameters)
    {
        return getPromisedAO().find(type, criteria, parameters);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, Query query)
    {
        return getPromisedAO().find(type, query);
    }

    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String field, Query query)
    {
        return getPromisedAO().find(type, field, query);
    }

    public <T extends RawEntity<K>, K> T[] findWithSQL(Class<T> type, String keyField, String sql, Object... parameters)
    {
        return getPromisedAO().findWithSQL(type, keyField, sql, parameters);
    }

    public <T extends RawEntity<K>, K> void stream(Class<T> type, Query query, EntityStreamCallback<T, K> streamCallback)
    {
        getPromisedAO().stream(type, query, streamCallback);
    }

    public <T extends RawEntity<K>, K> void stream(Class<T> type, EntityStreamCallback<T, K> streamCallback)
    {
        getPromisedAO().stream(type, streamCallback);
    }

    public <K> int count(Class<? extends RawEntity<K>> type)
    {
        return getPromisedAO().count(type);
    }

    public <K> int count(Class<? extends RawEntity<K>> type, String criteria, Object... parameters)
    {
        return getPromisedAO().count(type, criteria, parameters);
    }

    public <K> int count(Class<? extends RawEntity<K>> type, Query query)
    {
        return getPromisedAO().count(type, query);
    }

    public <T> T executeInTransaction(TransactionCallback<T> callback)
    {
        return getPromisedAO().executeInTransaction(callback);
    }

    public void restart(Promise<ActiveObjects> promise)
    {
        promisedAORef.set(promise);
    }
    
    public Bundle getBundle()
    {
        return bundle;
    }
    
    @Override
    public void awaitModelInitialization() throws ActiveObjectsInitException
    {
        getPromisedAO();
    }
}
