package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.plugin.ActiveObjectsModuleDescriptorFactory;
import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.BackupRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

/**
 * This is the listener that registers (and unregisters) the AO plugin for backup when needed.
 */
public final class BackupRegister
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Backup backup;

    /**
     * the current backup registry service
     */
    private BackupRegistry backupRegistry;

    public BackupRegister(Backup backup)
    {
        this.backup = checkNotNull(backup);
    }

    /**
     * This method is called when the {@link com.atlassian.activeobjects.spi.BackupRegistry} is available
     *
     * @param backupRegistry the backup registry that was made available.
     * @param properties the OSGi properties associated with the {@link com.atlassian.activeobjects.spi.BackupRegistry backup registry} service.
     */
    public void register(BackupRegistry backupRegistry, Map properties)
    {
        unregister(); // unregister first with the old service
        setBackupRegistry(backupRegistry);
        register();
    }

    /**
     * This method is called when the {@link com.atlassian.activeobjects.spi.BackupRegistry} is no longer available
     *
     * @param backupRegistry the backup registry that is no longer available
     * @param properties the OSGi properties associated with the {@link com.atlassian.activeobjects.spi.BackupRegistry backup registry} service.
     */
    public void unregister(BackupRegistry backupRegistry, Map properties)
    {
        unregister();
        setBackupRegistry(null); // no backup registry anymore
    }

    /**
     * This method is called when the Active Objects plugin gets deactivated for any reason
     *
     * @param aoModule the ao module descriptor factory (unused)
     * @param properties the OSGi properties associated with the {@link com.atlassian.activeobjects.plugin.ActiveObjectsModuleDescriptorFactory} service.
     */
    public void unregister(ActiveObjectsModuleDescriptorFactory aoModule, Map properties)
    {
        unregister();
    }

    private void register()
    {
        if (backupRegistry != null)
        {
            logger.info("Registering {} with {}", backup, backupRegistry);
            backupRegistry.register(backup);
        }
    }

    private void unregister()
    {
        if (backupRegistry != null)
        {
            logger.info("Un-registering {} with {}", backup, backupRegistry);
            backupRegistry.unregister(backup);
        }
    }

    private void setBackupRegistry(BackupRegistry backupRegistry)
    {
        this.backupRegistry = backupRegistry;
    }
}
