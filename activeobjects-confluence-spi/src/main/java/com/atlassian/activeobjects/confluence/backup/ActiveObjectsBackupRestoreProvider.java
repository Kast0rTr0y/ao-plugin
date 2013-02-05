package com.atlassian.activeobjects.confluence.backup;

import java.io.InputStream;
import java.io.OutputStream;

import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.confluence.importexport.ImportExportException;
import com.atlassian.confluence.importexport.plugin.BackupRestoreProvider;
import com.atlassian.event.api.EventPublisher;

/**
 * Backup and restore provider, implementing confluences backup and restore
 * module component and bridging to active objects backup component. 
 *
 */
public class ActiveObjectsBackupRestoreProvider implements BackupRestoreProvider
{
    private Backup backup;
    private EventPublisher eventPublisher;
	
	public void backup(OutputStream os) throws ImportExportException
	{
	    try
	    {
	        backup.save(os, new LoggingBackupProgressMonitor());
	    }
	    catch(Exception ex)
	    {
	        throw new ImportExportException(ex);
	    }
	}
	
	public void restore(InputStream is) throws ImportExportException
	{
	    try
        {
            backup.restore(is, new LoggingRestoreProgressMonitor());
        }
        catch(Exception ex)
        {
            throw new ImportExportException(ex);
        }
        finally
        {
            eventPublisher.publish(HotRestartEvent.INSTANCE);
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
}
