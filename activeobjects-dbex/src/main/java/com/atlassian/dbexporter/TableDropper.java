package com.atlassian.dbexporter;

import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.Table;

public interface TableDropper
{
    void drop(DatabaseInformation databaseInformation, Iterable<Table> tables, EntityNameProcessor entityNameProcessor);
}
