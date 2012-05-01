package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.progress.ProgressMonitor;

public interface TableCreator
{
    void create(DatabaseInformation databaseInformation, Iterable<Table> tables, EntityNameProcessor entityNameProcessor, ProgressMonitor monitor);
}
