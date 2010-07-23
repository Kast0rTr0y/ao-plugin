package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.sql.DataSourceProvider;

import javax.sql.DataSource;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * Creates a new instance of ActiveObjects given a dataSourceProvider
 */
public class DataSourceProviderActiveObjectsFactory implements ActiveObjectsFactory
{
    private final EntityManagerFactory entityManagerFactory;
    private final DataSourceProvider dataSourceProvider;

    public DataSourceProviderActiveObjectsFactory(EntityManagerFactory entityManagerFactory, DataSourceProvider dataSourceProvider)
    {
        this.entityManagerFactory = checkNotNull(entityManagerFactory);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
    }

    /**
     * Creates an {@link com.atlassian.activeobjects.external.ActiveObjects} using the
     * {@link com.atlassian.sal.api.sql.DataSourceProvider}
     * @param pluginKey the plugin key of the current plugin
     * @return a new configured, ready to go ActiveObjects instance
     * @throws ActiveObjectsPluginException if the data source obtained from the {@link com.atlassian.sal.api.sql.DataSourceProvider}
     * is {@code null}
     */
    public ActiveObjects create(PluginKey pluginKey)
    {
        // the data source from the application
        final DataSource dataSource = dataSourceProvider.getDataSource();
        if (dataSource == null)
        {
            throw new ActiveObjectsPluginException("No data source defined in the application");
        }
        return new EntityManagedActiveObjects(entityManagerFactory.getEntityManager(dataSource));
    }
}
