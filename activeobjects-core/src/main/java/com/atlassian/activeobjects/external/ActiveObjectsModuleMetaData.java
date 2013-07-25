package com.atlassian.activeobjects.external;

/**
 * This interface provides information about the state of the active objects module itself.
 *
 */
public interface ActiveObjectsModuleMetaData
{
    /**
     * Awaits initialization of the ActiveObjects model,this method will block until the active objects schema has been
     * initialized.
     * 
     * This method cannot be called from within an UpgradeTask
     * 
     * @throws ActiveObjectsPluginException if an exception occurred during initializations
     */
    void awaitInitialization();

    /**
     * Indicates if initialization has completed successfully. If this returns true a call to awaitInitialization will
     * return immediately and won't throw an exception.
     * 
     * @return true if initialized, false otherwise
     */
    boolean isInitialized();
}
