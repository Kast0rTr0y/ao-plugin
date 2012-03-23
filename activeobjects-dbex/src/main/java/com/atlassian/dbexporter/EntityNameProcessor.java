package com.atlassian.dbexporter;

public interface EntityNameProcessor
{
    /**
     * Processes the table name
     *
     * @param table the table name before processing
     * @return the table name after processing
     */
    String tableName(String table);

    /**
     * Processes the column name
     *
     * @param column the column name before processing
     * @return the column name after processing
     */
    String columnName(String column);
}
