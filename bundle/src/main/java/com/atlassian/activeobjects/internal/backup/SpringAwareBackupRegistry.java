package com.atlassian.activeobjects.internal.backup;

import com.atlassian.sal.api.backup.Backup;
import com.atlassian.sal.api.backup.BackupRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.service.ServiceUnavailableException;

import java.util.Collections;
import java.util.Set;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

/**
 * This is a spring OSGi aware backup registry that will "fail" gracefully if the backup service is not available
 */
public final class SpringAwareBackupRegistry implements BackupRegistry
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final BackupRegistry backupRegistry;

    public SpringAwareBackupRegistry(BackupRegistry backupRegistry)
    {
        this.backupRegistry = checkNotNull(backupRegistry);
    }

    public void register(Backup backup)
    {
        try
        {
            backupRegistry.register(backup);
        }
        catch (ServiceUnavailableException ignored)
        {
            logger.info("Service {} is not available, will not enable backup for plugins using active objects", BackupRegistry.class.getName());
            logger.debug("Here is the exception we got:", ignored);
        }
    }

    public void unregister(Backup backup)
    {
        try
        {
            backupRegistry.unregister(backup);
        }
        catch (ServiceUnavailableException ignored)
        {
            logger.info("Service {} is not available", BackupRegistry.class);
            logger.debug("Here is the exception we got:", ignored);
        }
    }

    public Set<Backup> getRegistered()
    {
        try
        {
            return backupRegistry.getRegistered();
        }
        catch (ServiceUnavailableException ignored)
        {
            logger.info("Service {} is not available", BackupRegistry.class);
            logger.debug("Here is the exception we got:", ignored);
            return Collections.emptySet();
        }
    }
}
