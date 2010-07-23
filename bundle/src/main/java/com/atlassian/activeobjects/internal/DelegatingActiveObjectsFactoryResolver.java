package com.atlassian.activeobjects.internal;

import java.util.List;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * ActiveObjectsFactoryResolver that accepts a list of resolvers which will be queried
 * to find an actual implementation.
 */
public final class DelegatingActiveObjectsFactoryResolver implements ActiveObjectsFactoryResolver
{
    private final List<ActiveObjectsFactoryResolver> resolvers;

    public DelegatingActiveObjectsFactoryResolver(List<ActiveObjectsFactoryResolver> resolvers)
    {
        this.resolvers = checkNotNull(resolvers);
    }

    public boolean accept(DataSourceType dataSourceType)
    {
        for (ActiveObjectsFactoryResolver resolver : resolvers)
        {
            if (resolver.accept(dataSourceType))
            {
                return true;
            }
        }
        return false;
    }

    public ActiveObjectsFactory get(DataSourceType dataSourceType) throws CannotResolveActiveObjectsFactoryException
    {
        for (ActiveObjectsFactoryResolver resolver : resolvers)
        {
            if (resolver.accept(dataSourceType))
            {
                return resolver.get(dataSourceType);
            }
        }
        throw new CannotResolveActiveObjectsFactoryException(dataSourceType);
    }
}
