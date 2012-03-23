package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.spi.RestoreProgressMonitor;
import com.atlassian.dbexporter.progress.ProgressMonitor;

import static com.google.common.base.Preconditions.*;

final class ActiveObjectsRestoreProgressMonitor implements ProgressMonitor
{
    private final RestoreProgressMonitor backupProgressMonitor;

    ActiveObjectsRestoreProgressMonitor(RestoreProgressMonitor restoreProgressMonitor)
    {
        this.backupProgressMonitor = checkNotNull(restoreProgressMonitor);
    }

    @Override
    public void begin(Object... args)
    {
        backupProgressMonitor.beginRestore();
    }

    @Override
    public void end(Object... args)
    {
        backupProgressMonitor.endRestore();
    }

    @Override
    public void begin(Task task, Object... args)
    {
        switch (task)
        {
            case DATABASE_INFORMATION:
                backupProgressMonitor.beginDatabaseInformationRestore();
                break;
            case TABLE_DEFINITION:
                backupProgressMonitor.beginTableDefinitionsRestore();
                break;
            case TABLES_DATA:
                backupProgressMonitor.beginTablesRestore();
                break;
            case TABLE_DATA:
                checkArgument(args.length == 1);
                checkArgument(args[0] instanceof String);
                backupProgressMonitor.beginTableDataRestore((String) args[0]);
                break;
            case TABLE_CREATION:
                checkArgument(args.length == 1);
                checkArgument(args[0] instanceof String);
                backupProgressMonitor.beginTableCreationRestore((String) args[0]);
                break;
            case TABLE_ROW:
                backupProgressMonitor.beginTableRowRestore();
                break;
        }
    }

    @Override
    public void end(Task task, Object... args)
    {
        switch (task)
        {
            case DATABASE_INFORMATION:
                backupProgressMonitor.endDatabaseInformationRestore();
                break;
            case TABLE_DEFINITION:
                backupProgressMonitor.endTableDefinitionsRestore();
                break;
            case TABLES_DATA:
                backupProgressMonitor.endTablesRestore();
                break;
            case TABLE_DATA:
                checkArgument(args.length == 1);
                checkArgument(args[0] instanceof String);
                backupProgressMonitor.endTableDataRestore((String) args[0]);
                break;
            case TABLE_CREATION:
                checkArgument(args.length == 1);
                checkArgument(args[0] instanceof String);
                backupProgressMonitor.endTableCreationRestore((String) args[0]);
                break;
            case TABLE_ROW:
                backupProgressMonitor.endTableRowRestore();
                break;
        }
    }

    @Override
    public void totalNumberOfTables(int size)
    {
        backupProgressMonitor.updateTotalNumberOfTablesToRestore(size);
    }
}
