package com.atlassian.activeobjects.internal;

public final class HomeDirectoryHsqlDbActiveObjectsFactoryResolver extends SingleDataSourceTypeActiveObjectsFactoryResolver
{
    HomeDirectoryHsqlDbActiveObjectsFactoryResolver(DatabaseDirectoryAwareActiveObjectsFactory activeObjectsFactory)
    {
        super(DataSourceType.HSQLDB, activeObjectsFactory);
    }
}
