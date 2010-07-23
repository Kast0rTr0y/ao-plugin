package com.atlassian.activeobjects.internal;

import javax.sql.DataSource;

/**
 * A class to resolve the driver class name from a given data source
 */
public interface DriverClassNameExtractor
{
    /**
     * Gets the driver class name from the data source
     *
     * @param dataSource the data source to resolve the driver class name from
     * @return a driver class name
     */
    String getDriverClassName(DataSource dataSource);
}
