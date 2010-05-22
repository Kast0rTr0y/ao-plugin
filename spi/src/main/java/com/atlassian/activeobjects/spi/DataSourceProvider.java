package com.atlassian.activeobjects.spi;

import javax.sql.DataSource;

/**
 * <p>An interface to allow SPI implementors to provide a {@code javax.sql.DataSource}
 * for use by the ActiveObjects plugin.</p>
 */
public interface DataSourceProvider {

    /**
     * Returns a {@code DataSource} implementation for ActiveObjects.
     * @return a {@code DataSource} implementation for ActiveObjects
     */
    DataSource getDataSource();

}
