package com.atlassian.activeobjects.refapp;

import com.atlassian.activeobjects.spi.AbstractDataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;

import javax.sql.DataSource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 */
public class RefappDataSourceProvider extends AbstractDataSourceProvider {
    private final DataSource dataSource;

    public RefappDataSourceProvider(DataSource dataSource) {
        this.dataSource = checkNotNull(dataSource);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public DatabaseType getDatabaseType() {
        return DatabaseType.HSQL;
    }
}
