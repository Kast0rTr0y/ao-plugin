package com.atlassian.activeobjects.internal;


/**
 * Resolves the {@link DataSourceType data source type} given a plugin key. The way
 * data source types are configured per plugin is implementation dependant.
 */
public interface DataSourceTypeResolver
{
    /**
     * Will return a configured {@link com.atlassian.activeobjects.internal.DataSourceType} for
     * the given plugin key
     *
     * @param prefix
     * @return a <em>non-{@code null}</em> DataSourceType, if none is 'configured' then a default
     *         shoud be provided.
     */
    DataSourceType getDataSourceType(Prefix prefix);
}
