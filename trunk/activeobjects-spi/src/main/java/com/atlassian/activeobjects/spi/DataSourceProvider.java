package com.atlassian.activeobjects.spi;

import javax.sql.DataSource;

/**
 * Gives access to the host application data source.
 */
public interface DataSourceProvider
{
    /**
     * @return the host application data source
     */
    DataSource getDataSource();

    /**
     * <p>Returns the database type.</p>
     * <p>Note: if {@link com.atlassian.activeobjects.spi.DatabaseType#UNKNOWN} is return it is left up to the client of
     * the data source provider to 'guess' the type of the database. It is strongly advised to implement this method so
     * that it never returns {@link com.atlassian.activeobjects.spi.DatabaseType#UNKNOWN}.</p>
     *
     * @return a valid database type
     * @see com.atlassian.activeobjects.spi.DatabaseType
     */
    DatabaseType getDatabaseType();

    /**
     * <p>The name of the schema used with this database.</p>
     * <p>This is especially import for SQL Server, PostgresQL and HSQLDB</p>
     *
     * @return the name of the schema to use, {@code null} if no schema is required.
     */
    String getSchema();
}
