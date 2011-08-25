package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.spi.DatabaseType;
import net.java.ao.DatabaseProvider;

import javax.sql.DataSource;

/**
 * A factory to create database provider given a data source
 */
public interface DatabaseProviderFactory
{
    DatabaseProvider getDatabaseProvider(DataSource dataSource, DatabaseType databaseType, String schema);
}
