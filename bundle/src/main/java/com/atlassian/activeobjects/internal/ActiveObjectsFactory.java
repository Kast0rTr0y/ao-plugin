package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;

/**
 * Factory to create instances of {@link com.atlassian.activeobjects.external.ActiveObjects}.
 */
public interface ActiveObjectsFactory
{
    /**
     * Tells whether the give data source type is supported by this factory, users should call this method before
     * calling {@link #create(DataSourceType, PluginKey)} to avoid an {@link IllegalStateException} being thrown.
     *
     * @param dataSourceType the type of data source to check whether supported
     * @return {@code true} if the {@link DataSourceType data source type} is supported.
     */
    boolean accept(DataSourceType dataSourceType);


    /**
     * Creates a <em>new</em> instance of {@link com.atlassian.activeobjects.external.ActiveObjects} each time it is called.
     *
     * @param dataSourceType the type of data source for which to create an Active Objects
     * @param pluginKey the plugin key of the current plugin
     * @return the new {@link com.atlassian.activeobjects.external.ActiveObjects}
     * @throws IllegalStateException is the type of data source is not supported by this factory
     * @see #accept(DataSourceType)
     */
    ActiveObjects create(DataSourceType dataSourceType, PluginKey pluginKey);
}
