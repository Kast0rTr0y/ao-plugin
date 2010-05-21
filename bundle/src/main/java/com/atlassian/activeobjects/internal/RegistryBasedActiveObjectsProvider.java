package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 *
 */
public class RegistryBasedActiveObjectsProvider implements ActiveObjectsProvider
{
    private final ActiveObjectsRegistry registry;
    private final ActiveObjectsFactoryResolver resolver;

    public RegistryBasedActiveObjectsProvider(ActiveObjectsRegistry registry, ActiveObjectsFactoryResolver resolver)
    {
        this.registry = checkNotNull(registry);
        this.resolver = checkNotNull(resolver);
    }

    public synchronized ActiveObjects get(String pluginKey)
    {
        ActiveObjects ao = registry.get(pluginKey);
        if (ao == null) // we need to create one
        {
            ao = registry.register(pluginKey, resolver.get(pluginKey).create());
        }
        return ao;
    }
}
