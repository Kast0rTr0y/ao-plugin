package com.atlassian.activeobjects.spi;

public interface BackupProgressMonitor
{
    void beginBackup();

    void endBackup();

    void beginDatabaseInformationBackup();

    void beginTableDefinitionsBackup();

    void beginTablesBackup();

    void beginTableBackup(String tableName);

    void updateTotalNumberOfTablesToBackup(int tableCount);

    void endDatabaseInformationBackup();

    void endTableDefinitionsBackup();

    void endTablesBackup();

    void endTableBackup(String tableName);
}
