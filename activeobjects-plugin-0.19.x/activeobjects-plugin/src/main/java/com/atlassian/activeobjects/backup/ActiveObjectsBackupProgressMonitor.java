package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.spi.BackupProgressMonitor;
import com.atlassian.dbexporter.progress.ProgressMonitor;

import static com.google.common.base.Preconditions.*;

final class ActiveObjectsBackupProgressMonitor implements ProgressMonitor
{
    private final BackupProgressMonitor backupProgressMonitor;

    ActiveObjectsBackupProgressMonitor(BackupProgressMonitor backupProgressMonitor)
    {
        this.backupProgressMonitor = checkNotNull(backupProgressMonitor);
    }

    @Override
    public void begin(Object... args)
    {
        backupProgressMonitor.beginBackup();
    }

    @Override
    public void end(Object... args)
    {
        backupProgressMonitor.endBackup();
    }

    @Override
    public void begin(Task task, Object... args)
    {
        switch (task)
        {
            case DATABASE_INFORMATION:
                backupProgressMonitor.beginDatabaseInformationBackup();
                break;
            case TABLE_DEFINITION:
                backupProgressMonitor.beginTableDefinitionsBackup();
                break;
            case TABLES_DATA:
                backupProgressMonitor.beginTablesBackup();
                break;
            case TABLE_DATA:
                checkArgument(args.length == 1);
                checkArgument(args[0] instanceof String);
                backupProgressMonitor.beginTableBackup((String) args[0]);
                break;
        }
    }

    @Override
    public void end(Task task, Object... args)
    {
        switch (task)
        {
            case DATABASE_INFORMATION:
                backupProgressMonitor.endDatabaseInformationBackup();
                break;
            case TABLE_DEFINITION:
                backupProgressMonitor.endTableDefinitionsBackup();
                break;
            case TABLES_DATA:
                backupProgressMonitor.endTablesBackup();
                break;
            case TABLE_DATA:
                checkArgument(args.length == 1);
                checkArgument(args[0] instanceof String);
                backupProgressMonitor.endTableBackup((String) args[0]);
                break;
        }
    }

    @Override
    public void totalNumberOfTables(int size)
    {
        backupProgressMonitor.updateTotalNumberOfTablesToBackup(size);
    }
}
