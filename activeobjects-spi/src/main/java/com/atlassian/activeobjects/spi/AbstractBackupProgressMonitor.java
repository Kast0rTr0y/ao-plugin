package com.atlassian.activeobjects.spi;

public abstract class AbstractBackupProgressMonitor implements BackupProgressMonitor
{
    @Override
    public void beginBackup()
    {
    }

    @Override
    public void endBackup()
    {
    }

    @Override
    public void beginDatabaseInformationBackup()
    {
    }

    @Override
    public void beginTableDefinitionsBackup()
    {
    }

    @Override
    public void beginTablesBackup()
    {
    }

    @Override
    public void beginTableBackup(String tableName)
    {
    }

    @Override
    public void updateTotalNumberOfTablesToBackup(int tableCount)
    {
    }

    @Override
    public void endDatabaseInformationBackup()
    {
    }

    @Override
    public void endTableDefinitionsBackup()
    {
    }

    @Override
    public void endTablesBackup()
    {
    }

    @Override
    public void endTableBackup(String tableName)
    {
    }
}
