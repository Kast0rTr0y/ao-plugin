package com.atlassian.activeobjects.spi;

import javax.sql.DataSource;

/**
 *
 */
public abstract class AbstractDataSourceProvider implements DataSourceProvider
{
    public abstract DataSource getDataSource();

    public DatabaseType getDatabaseType()
    {
        return DatabaseType.UNKNOWN;
    }
}
