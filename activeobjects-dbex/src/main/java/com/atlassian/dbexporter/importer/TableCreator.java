package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.Table;

public interface TableCreator
{
    void create(Iterable<Table> tables, EntityNameProcessor entityNameProcessor);
}
