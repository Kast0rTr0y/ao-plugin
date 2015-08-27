package com.atlassian.activeobjects.spi;

import com.atlassian.tenancy.api.Tenant;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

/**
 * Gives access to the host application data source.
 */
public interface TenantAwareDataSourceProvider {
    /**
     * Provide host application data source associated with a tenant.
     *
     * This data source will be used for the entire lifetime of {@link com.atlassian.activeobjects.external.ActiveObjects}
     */
    @Nonnull
    DataSource getDataSource(@Nonnull final Tenant tenant);

    /**
     * <p>Returns the database type for the tenant</p>
     * <p>Note: if {@link com.atlassian.activeobjects.spi.DatabaseType#UNKNOWN} is return it is left up to the client of
     * the data source provider to 'guess' the type of the database. It is strongly advised to implement this method so
     * that it never returns {@link com.atlassian.activeobjects.spi.DatabaseType#UNKNOWN}.</p>
     *
     * @return a valid database type
     * @see com.atlassian.activeobjects.spi.DatabaseType
     */
    @Nonnull
    DatabaseType getDatabaseType(@Nonnull final Tenant tenant);

    /**
     * <p>The name of the schema used with this database for the tenant.</p>
     * <p>This is especially import for SQL Server, PostgresQL and HSQLDB</p>
     *
     * @return the name of the schema to use, {@code null} if no schema is required.
     */
    String getSchema(@Nonnull final Tenant tenant);
}
