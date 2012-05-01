package com.atlassian.dbexporter;

public final class NoOpEntityNameProcessor implements EntityNameProcessor
{
    @Override
    public String tableName(String table)
    {
        return table;
    }

    @Override
    public String columnName(String column)
    {
        return column;
    }
}
