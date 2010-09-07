package com.atlassian.activeobjects.spi;

/**
 * This is the service that the active objects plugin will use to register for backup (and restore)
 */
public interface BackupRegistry
{
    /**
     * Registers a backup, this is typically done at plugin install/enable.
     *
     * @param backup the backup to register.
     */
    void register(Backup backup);

    /**
     * <p>Un-register a backup, this is typically done at plugin uninstall/disable.</p>
     * <p>Note that the {@link Backup} object needs to be <em>equal</em>
     * to the one used when {@link #register(Backup) registering} for this method to work properly.</p>
     *
     * @param backup the backup to uninstall.
     */
    void unregister(Backup backup);
}
