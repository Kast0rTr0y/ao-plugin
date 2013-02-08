package com.atlassian.activeobjects.confluence.backup;

import java.io.InputStream;
import java.io.OutputStream;

import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.activeobjects.spi.TransactionSynchronisationManager;
import com.atlassian.confluence.importexport.ImportExportException;
import com.atlassian.confluence.importexport.plugin.BackupRestoreProvider;
import com.atlassian.event.api.EventPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Backup and restore provider, implementing confluences backup and restore
 * module component and bridging to active objects backup component. 
 *
 */
public class ActiveObjectsBackupRestoreProvider implements BackupRestoreProvider
{
    private Backup backup;
    private EventPublisher eventPublisher;
    private TransactionSynchronisationManager transactionSyncManager;

    private static final Logger log = LoggerFactory.getLogger(ActiveObjectsBackupRestoreProvider.class);

    public void backup(OutputStream os) throws ImportExportException
    {
        try
        {
            backup.save(os, new LoggingBackupProgressMonitor());
        }
        catch (Exception ex)
        {
            throw new ImportExportException(ex);
        }
    }

    public void restore(InputStream is) throws ImportExportException
    {
        try
        {
	        Runnable restartAoCallback = new Runnable()
            {
                @Override
                public void run()
                {
                    log.info("Firing active objects hot restart event.");
                    eventPublisher.publish(HotRestartEvent.INSTANCE);
                }
            };

            transactionSyncManager.runOnSuccessfulCommit(restartAoCallback);
            transactionSyncManager.runOnRollBack(restartAoCallback);

            backup.restore(is, new LoggingRestoreProgressMonitor());
        }
        catch(Exception ex)
        {
            throw new ImportExportException(ex);
        }
    }

    public void setBackup(Backup backup)
    {
        this.backup = backup;
    }

    public void setEventPublisher(EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
    }

    public void setTransactionSynchManager(TransactionSynchronisationManager tranSyncManager)
    {
        this.transactionSyncManager = tranSyncManager;
    }
}
