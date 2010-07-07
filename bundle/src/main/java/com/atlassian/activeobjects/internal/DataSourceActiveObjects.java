package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.spi.DataSourceProvider;
import net.java.ao.DatabaseProvider;
import net.java.ao.EntityManager;

import java.sql.SQLException;

/**
 * Simply delegates back to the underlying {@link net.java.ao.EntityManager} using a datasource 
 */
public class DataSourceActiveObjects extends EntityManagedActiveObjects
{
    public DataSourceActiveObjects(DataSourceProvider dataSourceProvider, String pluginKey) throws SQLException
    {
        super(new EntityManager(getDatabaseProvider(dataSourceProvider), true), pluginKey);
    }

    private static DatabaseProvider getDatabaseProvider(DataSourceProvider dataSourceProvider) throws SQLException
    {
        return DatabaseProviders.getProviderFromDataSource( dataSourceProvider.getDataSource() );
    }

}
