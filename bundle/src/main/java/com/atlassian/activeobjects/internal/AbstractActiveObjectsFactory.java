package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;

/**
 * Abstract implementation of {@link com.atlassian.activeobjects.internal.ActiveObjectsFactory} that implements the
 * basic contract for a single {@link com.atlassian.activeobjects.internal.DataSourceType}.
 */
abstract class AbstractActiveObjectsFactory implements ActiveObjectsFactory
{
    private final DataSourceType supportedDataSourceType;

    AbstractActiveObjectsFactory(DataSourceType dataSourceType)
    {
        this.supportedDataSourceType = dataSourceType;
    }

    public final boolean accept(DataSourceType dataSourceType)
    {
        return supportedDataSourceType.equals(dataSourceType);
    }

    public final ActiveObjects create(DataSourceType dataSourceType, PluginKey pluginKey)
    {
        if (!accept(dataSourceType))
        {
            throw new IllegalStateException(dataSourceType + " is not supported. Did you can #accept(DataSourceType) before calling me?");
        }
        return create(pluginKey);
    }

    /**
     * This has the same contract as {@link #create(DataSourceType, PluginKey)} except that checking the data source
     * type has already been taken care of.
     *
     * @param pluginKey the key of the plugin for which to create an Active Objects instance
     * @return the new {@link com.atlassian.activeobjects.external.ActiveObjects}
     */
    protected abstract ActiveObjects create(PluginKey pluginKey);
}
