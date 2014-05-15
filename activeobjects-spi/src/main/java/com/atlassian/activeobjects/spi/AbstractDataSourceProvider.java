package com.atlassian.activeobjects.spi;

import com.atlassian.tenancy.api.Tenant;

import javax.annotation.Nonnull;

/**
 * This class provides a partial implementation of the {@link com.atlassian.activeobjects.spi.DataSourceProvider}
 * where {@link #getDatabaseType()} always returns {@link com.atlassian.activeobjects.spi.DatabaseType#UNKNOWN}.
 */
public abstract class AbstractDataSourceProvider implements DataSourceProvider
{
    /**
     * Always returns com.atlassian.activeobjects.spi.DatabaseType#UNKNOWN
     *
     * @return {@link com.atlassian.activeobjects.spi.DatabaseType#UNKNOWN}
     */
    @Override
    @Nonnull
    public DatabaseType getDatabaseType(@Nonnull final Tenant tenant)
    {
        return DatabaseType.UNKNOWN;
    }

    @Override
    public String getSchema(@Nonnull final Tenant tenant)
    {
        return null; // use the default schema configured for the user
    }
}
