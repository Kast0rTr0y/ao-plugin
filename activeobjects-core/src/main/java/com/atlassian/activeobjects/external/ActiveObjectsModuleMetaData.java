package com.atlassian.activeobjects.external;

import com.atlassian.activeobjects.spi.DatabaseType;

/**
 * This interface provides information about the state of the active objects module itself.
 *
 * @since 0.24
 * @deprecated since 0.26 This no longer makes sense in a tenant aware environment, as these three methods all depend
 * on the data source (for the tenant) that is provided by the application at the time.
 */
@Deprecated
public interface ActiveObjectsModuleMetaData
{
    /**
     * Awaits initialization of the ActiveObjects model,this method will block until the active objects schema has been
     * initialized.
     * 
     * This method cannot be called from within an UpgradeTask
     * 
     * @throws ActiveObjectsPluginException if an exception occurred during initializations
     * @deprecated since 0.26
     */
    @Deprecated
    void awaitInitialization();

    /**
     * Indicates if initialization has completed successfully. If this returns true a call to awaitInitialization will
     * return immediately and won't throw an exception.
     * 
     * @return true if initialized, false otherwise
     * @deprecated since 0.26
     */
    @Deprecated
    boolean isInitialized();

    /**
     * 
     * @return the configured database type that active objects is using
     * @deprecated since 0.26
     */
    @Deprecated
    DatabaseType getDatabaseType();
}
