package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;

/**
 * A delegating factory that will check multiple factories to achieve its goal.
 */
public final class DelegatingActiveObjectsFactory implements ActiveObjectsFactory
{
    private final ImmutableSet<ActiveObjectsFactory> factories;

    public DelegatingActiveObjectsFactory(Collection<ActiveObjectsFactory> factories)
    {
        this.factories = ImmutableSet.<ActiveObjectsFactory>builder().addAll(factories).build();
    }

    public boolean accept(DataSourceType dataSourceType)
    {
        for (ActiveObjectsFactory factory : factories)
        {
            if (factory.accept(dataSourceType))
            {
                return true;
            }
        }
        return false;
    }

    public ActiveObjects create(DataSourceType dataSourceType, PluginKey pluginKey)
    {
        for (ActiveObjectsFactory factory : factories)
        {
            if (factory.accept(dataSourceType))
            {
                return factory.create(dataSourceType, pluginKey);
            }
        }
        throw new IllegalStateException("Could not find a factory for this data source type, " + dataSourceType + ", " +
                "did you call #accept(DataSourceType) before calling me?");
    }
}
