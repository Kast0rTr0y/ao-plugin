package com.atlassian.activeobjects.external;

import com.atlassian.activeobjects.spi.DatabaseType;

/**
 * This interface provides information about the state of the active objects module itself, in the context of the
 * current data source.
 *
 * @since 0.24
 */
public interface ActiveObjectsModuleMetaData
{
    /**
     * Awaits initialization of the ActiveObjects model,this method will block until the active objects schema has been
     * initialized for the current data source.
     * 
     * This method cannot be called from within an UpgradeTask
     *
     * @throws com.atlassian.activeobjects.external.NoDataSourceException
     */
    void awaitInitialization();

    /**
     * Indicates if initialization has completed successfully. If this returns true a call to awaitInitialization will
     * return immediately and won't throw an exception.
     * 
     * @return true if initialized, false otherwise
     * @throws com.atlassian.activeobjects.external.NoDataSourceException
     */
    boolean isInitialized();

    /**
     * @return the configured database type that active objects is using
     * @throws com.atlassian.activeobjects.external.NoDataSourceException
     */
    DatabaseType getDatabaseType();
}
