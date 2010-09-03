package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 *
 */
public class RegistryBasedActiveObjectsProvider implements ActiveObjectsProvider
{
    private final ActiveObjectsRegistry registry;
    private final ActiveObjectsFactory activeObjectsFactory;

    public RegistryBasedActiveObjectsProvider(ActiveObjectsRegistry registry, ActiveObjectsFactory activeObjectsFactory)
    {
        this.registry = checkNotNull(registry);
        this.activeObjectsFactory = checkNotNull(activeObjectsFactory);
    }

    public synchronized ActiveObjects get(ActiveObjectsConfiguration configuration)
    {
        ActiveObjects ao = registry.get(configuration);
        if (ao == null) // we need to create one
        {
            ao = registry.register(configuration, activeObjectsFactory.create(configuration));
        }
        return ao;
    }
}
