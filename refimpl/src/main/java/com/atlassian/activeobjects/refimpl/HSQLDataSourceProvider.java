package com.atlassian.activeobjects.refimpl;

import com.atlassian.activeobjects.spi.DataSourceProvider;

import javax.sql.DataSource;

/**
 * Provides a HSQL-backed {@code DataSource} implementation.
 */
public class HSQLDataSourceProvider implements DataSourceProvider {

    private HSQLDataSource hsqlDataSource;

    public HSQLDataSourceProvider(HSQLDataSource hsqlDataSource) {
        this.hsqlDataSource = hsqlDataSource;
    }

    public DataSource getDataSource() {
        return hsqlDataSource;
    }
}
