package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;

public class CannotResolveActiveObjectsFactoryException extends ActiveObjectsPluginException
{
    private final DataSourceType dataSourceType;

    public CannotResolveActiveObjectsFactoryException(DataSourceType dataSourceType)
    {
        this.dataSourceType = dataSourceType;
    }

    public DataSourceType getDataSourceType()
    {
        return dataSourceType;
    }

    @Override
    public String getMessage()
    {
        return new StringBuilder()
                .append("Could not resolve active objects factory for data source type <")
                .append(dataSourceType)
                .append(">")
                .toString();
    }
}
