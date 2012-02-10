package com.atlassian.dbexporter;

import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.importer.ForeignKeyManager;

public interface TableDropper
{
    /**
     * Drops tables.
     * Foreign keys must be guaranteed to be deleted before calling this method. {@link ForeignKeyManager}
     */
    void drop(DatabaseInformation databaseInformation, Iterable<Table> tables, EntityNameProcessor entityNameProcessor);
}
