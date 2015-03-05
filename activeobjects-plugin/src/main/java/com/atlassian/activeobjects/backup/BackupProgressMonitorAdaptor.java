package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.spi.BackupProgressMonitor;

public class BackupProgressMonitorAdaptor implements com.atlassian.dbexporter.api.BackupProgressMonitor
{
    private final BackupProgressMonitor delegate;

    public BackupProgressMonitorAdaptor(BackupProgressMonitor delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void beginBackup()
    {
        delegate.beginBackup();
    }

    @Override
    public void endBackup()
    {
        delegate.endBackup();
    }

    @Override
    public void beginDatabaseInformationBackup()
    {
        delegate.beginDatabaseInformationBackup();
    }

    @Override
    public void beginTableDefinitionsBackup()
    {
        delegate.beginTableDefinitionsBackup();
    }

    @Override
    public void beginTablesBackup()
    {
        delegate.beginTablesBackup();
    }

    @Override
    public void beginTableBackup(final String tableName)
    {
        delegate.beginTableBackup(tableName);
    }

    @Override
    public void updateTotalNumberOfTablesToBackup(final int tableCount)
    {
        delegate.updateTotalNumberOfTablesToBackup(tableCount);
    }

    @Override
    public void endDatabaseInformationBackup()
    {
        delegate.endDatabaseInformationBackup();
    }

    @Override
    public void endTableDefinitionsBackup()
    {
        delegate.endTableDefinitionsBackup();
    }

    @Override
    public void endTablesBackup()
    {
        delegate.endTablesBackup();
    }

    @Override
    public void endTableBackup(final String tableName)
    {
        delegate.endTableBackup(tableName);
    }
}
