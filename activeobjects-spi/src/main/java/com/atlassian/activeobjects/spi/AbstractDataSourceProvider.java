package com.atlassian.activeobjects.spi;

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
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.UNKNOWN;
    }
}
