package com.atlassian.activeobjects.internal;

/**
 * Types of data sources that active objects plugin can use
 */
public enum DataSourceType
{
    /**
     * The data source comes from the configured {@link com.atlassian.sal.api.sql.DataSourceProvider}
     * provided by the host application.
     */
    APPLICATION,

    /**
     * The data source is an HSQL database 'somewhere' in the home directory of the application
     */
    HSQLDB
}
