package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
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

    public boolean accept(ActiveObjectsConfiguration configuration)
    {
        for (ActiveObjectsFactory factory : factories)
        {
            if (factory.accept(configuration))
            {
                return true;
            }
        }
        return false;
    }

    public ActiveObjects create(ActiveObjectsConfiguration configuration)
    {
        for (ActiveObjectsFactory factory : factories)
        {
            if (factory.accept(configuration))
            {
                return factory.create(configuration);
            }
        }
        throw new IllegalStateException("Could not find a factory for this configuration, " + configuration + ", " +
                "did you call #accept(ActiveObjectsConfiguration) before calling me?");
    }
}
