package com.atlassian.activeobjects.spi;

public interface RestoreProgressMonitor
{
    void beginRestore();

    void endRestore();

    void beginDatabaseInformationRestore();

    void beginTableDefinitionsRestore();

    void beginTablesRestore();

    void beginTableDataRestore(String tableName);

    void beginTableCreationRestore(String tableName);

    void beginTableRowRestore();

    void endDatabaseInformationRestore();

    void endTableDefinitionsRestore();

    void endTablesRestore();

    void endTableDataRestore(String tableName);

    void endTableCreationRestore(String tableName);

    void endTableRowRestore();

    void updateTotalNumberOfTablesToRestore(int tableCount);
}
