package com.atlassian.activeobjects.spi;

public abstract class AbstractRestoreProgressMonitor implements RestoreProgressMonitor
{
    @Override
    public void beginRestore()
    {
    }

    @Override
    public void endRestore()
    {
    }

    @Override
    public void beginDatabaseInformationRestore()
    {
    }

    @Override
    public void beginTableDefinitionsRestore()
    {
    }

    @Override
    public void beginTablesRestore()
    {
    }

    @Override
    public void beginTableDataRestore(String tableName)
    {
    }

    @Override
    public void beginTableCreationRestore(String tableName)
    {
    }

    @Override
    public void beginTableRowRestore()
    {
    }

    @Override
    public void endDatabaseInformationRestore()
    {
    }

    @Override
    public void endTableDefinitionsRestore()
    {
    }

    @Override
    public void endTablesRestore()
    {
    }

    @Override
    public void endTableDataRestore(String tableName)
    {
    }

    @Override
    public void endTableCreationRestore(String tableName)
    {
    }

    @Override
    public void endTableRowRestore()
    {
    }

    @Override
    public void updateTotalNumberOfTablesToRestore(int tableCount)
    {
    }
}