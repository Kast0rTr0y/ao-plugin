package com.atlassian.activeobjects.internal;

/**
 * Resolver that will give access to the correct {@link com.atlassian.activeobjects.internal.ActiveObjectsFactory} given a plugin key.
 * Getting the correct factory might be driven, for example, by the configuration of the plugin.
 */
public interface ActiveObjectsFactoryResolver
{
    /**
     * Tells wether or not the resolver will be able to {@link #get(DataSourceType)} the
     * {@link com.atlassian.activeobjects.internal.ActiveObjectsFactory}
     *
     * @param dataSourceType the type of data source that the looked up factory must support
     * @return {@code true} if an {@link ActiveObjectsFactory active objects factory} can be resolved, {@code false} otherwise.
     */
    boolean accept(DataSourceType dataSourceType);

    /**
     * Gets the {@link com.atlassian.activeobjects.internal.ActiveObjectsFactory} associated with the plugin referenced by {@code pluginKey}
     *
     * @param dataSourceType the type of data source that the looked up factory must support
     * @return the 'correct' non-null factory if the {@link #accept(DataSourceType)} returns true for the same {@code dataSourceType}
     * @throws CannotResolveActiveObjectsFactoryException
     *          if {@link #accept(DataSourceType)} returns {@code false} for the same {@code pluginKey}
     */
    ActiveObjectsFactory get(DataSourceType dataSourceType) throws CannotResolveActiveObjectsFactoryException;
}
