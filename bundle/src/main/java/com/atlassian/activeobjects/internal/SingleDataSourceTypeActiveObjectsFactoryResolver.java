package com.atlassian.activeobjects.internal;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

abstract class SingleDataSourceTypeActiveObjectsFactoryResolver implements ActiveObjectsFactoryResolver
{
    private final ActiveObjectsFactory activeObjectsFactory;
    private final DataSourceType acceptedDataSourceType;

    SingleDataSourceTypeActiveObjectsFactoryResolver(DataSourceType acceptedDataSourceType, ActiveObjectsFactory activeObjectsFactory)
    {
        this.acceptedDataSourceType = checkNotNull(acceptedDataSourceType);
        this.activeObjectsFactory = checkNotNull(activeObjectsFactory);
    }

    /**
     * Returns {@code true} if {@code dataSoureType} is {@link DataSourceType#APPLICATION}
     *
     * @param dataSourceType the type of data source that the looked up factory must support
     * @return {@code true} if {@code dataSoureType} is {@link DataSourceType#APPLICATION}
     */
    public final boolean accept(DataSourceType dataSourceType)
    {
        return acceptedDataSourceType.equals(dataSourceType);
    }

    /**
     * @param dataSourceType the type of data source that the looked up factory must support
     * @return an
     * @throws CannotResolveActiveObjectsFactoryException
     *
     */
    public final ActiveObjectsFactory get(DataSourceType dataSourceType) throws CannotResolveActiveObjectsFactoryException
    {
        if (accept(dataSourceType))
        {
            return activeObjectsFactory;
        }
        else
        {
            throw new CannotResolveActiveObjectsFactoryException(dataSourceType);
        }
    }
}
