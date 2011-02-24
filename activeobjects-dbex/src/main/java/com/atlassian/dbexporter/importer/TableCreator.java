package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.Table;

public interface TableCreator
{
    void create(Iterable<Table> tables, Context context);
}
