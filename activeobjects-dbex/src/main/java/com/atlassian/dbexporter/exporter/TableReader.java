package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.Table;

public interface TableReader
{
    Iterable<Table> read(Context context);
}
