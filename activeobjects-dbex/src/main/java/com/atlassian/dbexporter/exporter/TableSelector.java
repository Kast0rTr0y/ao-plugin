package com.atlassian.dbexporter.exporter;

public interface TableSelector
{
    boolean accept(String tableName);
}
