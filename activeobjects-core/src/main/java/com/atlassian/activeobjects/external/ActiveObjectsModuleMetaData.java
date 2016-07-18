package com.atlassian.activeobjects.external;

import com.atlassian.activeobjects.spi.DatabaseType;
import net.java.ao.RawEntity;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This interface provides information about the state of the active objects module itself, in the context of the
 * current data source.
 *
 * @since 0.24
 */
public interface ActiveObjectsModuleMetaData {
    /**
     * Awaits initialization of the ActiveObjects model,this method will block until the active objects schema has been
     * initialized for the current data source.
     *
     * This method cannot be called from within an UpgradeTask
     *
     * Blocks indefinitely.
     *
     * @throws com.atlassian.activeobjects.external.NoDataSourceException
     * @throws com.atlassian.activeobjects.ActiveObjectsInitException     if an exception occurred during initializations
     */
    void awaitInitialization() throws ExecutionException, InterruptedException;

    /**
     * See {@link #awaitInitialization()}
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @since 0.26
     */
    void awaitInitialization(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Indicates if initialization has completed successfully. If this returns true a call to awaitInitialization will
     * return immediately and won't throw an exception, providing a tenant is present.
     *
     * @return true if initialized, false otherwise
     */
    boolean isInitialized();

    /**
     * @return the configured database type that active objects is using
     * @throws com.atlassian.activeobjects.external.NoDataSourceException
     */
    DatabaseType getDatabaseType();

    /**
     * Indicates whether there is a data source (i.e. a tenant) present.
     *
     * If this returns true, calls to {@link #awaitInitialization()},
     * {@link #awaitInitialization(long, java.util.concurrent.TimeUnit)} and {@link #getDatabaseType()} will not throw a
     * {@link com.atlassian.activeobjects.external.NoDataSourceException}
     *
     * @since 0.26
     */
    boolean isDataSourcePresent();

    /**
     * Checks if the table corresponding to given type exists in the database.
     *
     * @param type to check against
     * @return true if the table exists, false otherwise
     * @since 1.2.1
     */
    boolean isTablePresent(Class<? extends RawEntity<?>> type);
}
