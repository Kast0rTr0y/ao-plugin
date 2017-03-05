package com.atlassian.activeobjects.spi;

import javax.annotation.Nullable;
import javax.sql.DataSource;

/**
 * Gives access to the host application data source.
 *
 * Products that are tenanted should instead implement {@link TenantAwareDataSourceProvider}.
 */
public interface DataSourceProvider {

    /**
     * Returns the host application's SQL data source.
     *
     * @return see above
     */
    DataSource getDataSource();

    /**
     * <p>Returns the database type.</p>
     * <p>Note: if {@link com.atlassian.activeobjects.spi.DatabaseType#UNKNOWN} is returned, it is up to the
     * calling code to 'guess' the type of the database. It is strongly advised to implement this method so
     * that it never returns {@link com.atlassian.activeobjects.spi.DatabaseType#UNKNOWN}.</p>
     *
     * @return a valid database type
     */
    DatabaseType getDatabaseType();

    /**
     * <p>Returns the name of the schema used with this database.</p>
     * <p>This is especially important for SQL Server, Postgres, and HSQLDB.</p>
     *
     * @return the name of the schema to use, {@code null} if no schema is required.
     */
    @Nullable
    String getSchema();
}
