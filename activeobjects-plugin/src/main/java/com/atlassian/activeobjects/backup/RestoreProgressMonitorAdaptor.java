package com.atlassian.activeobjects.backup;


import com.atlassian.activeobjects.spi.RestoreProgressMonitor;

public class RestoreProgressMonitorAdaptor implements com.atlassian.dbexporter.api.RestoreProgressMonitor
{
    private final RestoreProgressMonitor delegate;

    public RestoreProgressMonitorAdaptor(RestoreProgressMonitor delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void beginRestore()
    {
        delegate.beginRestore();
    }

    @Override
    public void endRestore()
    {
        delegate.endRestore();
    }

    @Override
    public void beginDatabaseInformationRestore()
    {
        delegate.beginDatabaseInformationRestore();
    }

    @Override
    public void beginTableDefinitionsRestore()
    {
        delegate.beginTableDefinitionsRestore();
    }

    @Override
    public void beginTablesRestore()
    {
        delegate.beginTablesRestore();
    }

    @Override
    public void beginTableDataRestore(final String tableName)
    {
        delegate.beginTableDataRestore(tableName);
    }

    @Override
    public void beginTableCreationRestore(final String tableName)
    {
        delegate.beginTableCreationRestore(tableName);
    }

    @Override
    public void beginTableRowRestore()
    {
        delegate.beginTableRowRestore();
    }

    @Override
    public void endDatabaseInformationRestore()
    {
        delegate.endDatabaseInformationRestore();
    }

    @Override
    public void endTableDefinitionsRestore()
    {
        delegate.endTableDefinitionsRestore();
    }

    @Override
    public void endTablesRestore()
    {
        delegate.endTablesRestore();
    }

    @Override
    public void endTableDataRestore(final String tableName)
    {
        delegate.endTableDataRestore(tableName);
    }

    @Override
    public void endTableCreationRestore(final String tableName)
    {
        delegate.endTableCreationRestore(tableName);
    }

    @Override
    public void endTableRowRestore()
    {
        delegate.endTableRowRestore();
    }

    @Override
    public void updateTotalNumberOfTablesToRestore(final int tableCount)
    {delegate.updateTotalNumberOfTablesToRestore(tableCount);}
}
