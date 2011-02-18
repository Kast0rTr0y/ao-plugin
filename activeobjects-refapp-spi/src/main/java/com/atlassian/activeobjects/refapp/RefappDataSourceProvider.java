package com.atlassian.activeobjects.refapp;

import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;

import javax.sql.DataSource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 */
public class RefappDataSourceProvider implements DataSourceProvider
{
    private final DataSource dataSource;

    public RefappDataSourceProvider(DataSource dataSource)
    {
        this.dataSource = checkNotNull(dataSource);
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public DatabaseType getDatabaseType()
    {
        return DatabaseType.HSQL;
    }
}