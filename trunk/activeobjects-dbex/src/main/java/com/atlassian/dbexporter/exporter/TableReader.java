package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.Table;

public interface TableReader
{
    Iterable<Table> read(DatabaseInformation databaseInformation, EntityNameProcessor entityNameProcessor);
}
