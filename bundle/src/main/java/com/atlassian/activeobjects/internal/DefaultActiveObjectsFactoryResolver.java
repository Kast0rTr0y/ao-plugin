package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.sal.api.ApplicationProperties;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 *
 */
public class DefaultActiveObjectsFactoryResolver implements ActiveObjectsFactoryResolver
{
    private final DataSourceProvider dataSourceProvider;

    public DefaultActiveObjectsFactoryResolver(DataSourceProvider dataSourceProvider)
    {
        this.dataSourceProvider = dataSourceProvider;
    }

    public ActiveObjectsFactory get(String pluginKey)
    {
        return new DataSourceActiveObjectsFactory(pluginKey, dataSourceProvider);
    }
}
