package com.atlassian.activeobjects.internal;

import static com.atlassian.activeobjects.internal.DataSourceType.APPLICATION;

/**
 * Active objects factory resolver which handles data source of type {@link DataSourceType#APPLICATION}
 */
public class ApplicationDataSourceProviderActiveObjectsFactoryResolver extends SingleDataSourceTypeActiveObjectsFactoryResolver
{
    public ApplicationDataSourceProviderActiveObjectsFactoryResolver(DataSourceProviderActiveObjectsFactory activeObjectsFactory)
    {
        super(APPLICATION, activeObjectsFactory);
    }
}
