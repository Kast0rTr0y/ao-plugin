package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.Table;

public interface ForeignKeyManager
{
    void create(Iterable<ForeignKey> foreignKeys, EntityNameProcessor entityNameProcessor);
    
    /**
     * Drop all foreign keys to and from all tables. This includes fields from this table which lead to PKs of foreign tables.
     * @throws ImportExportException if the operation fails
     * @return a new set of tables without the foreign keys
     */
    Iterable<Table> dropForTables(Iterable<Table> tables, EntityNameProcessor entityNameProcessor);    
}
