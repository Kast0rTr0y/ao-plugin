package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 *
 */
public class RegistryBasedActiveObjectsProvider implements ActiveObjectsProvider
{
    private final ActiveObjectsRegistry registry;
    private final ActiveObjectsFactory activeObjectsFactory;
    private final DataSourceTypeResolver dataSourceTypeResolver;

    public RegistryBasedActiveObjectsProvider(ActiveObjectsRegistry registry, ActiveObjectsFactory activeObjectsFactory, DataSourceTypeResolver dataSourceTypeResolver)
    {
        this.registry = checkNotNull(registry);
        this.activeObjectsFactory = checkNotNull(activeObjectsFactory);
        this.dataSourceTypeResolver = checkNotNull(dataSourceTypeResolver);
    }

    public synchronized ActiveObjects get(PluginKey pluginKey)
    {
        ActiveObjects ao = registry.get(pluginKey);
        if (ao == null) // we need to create one
        {
            ao = registry.register(pluginKey, activeObjectsFactory.create(dataSourceTypeResolver.getDataSourceType(pluginKey), pluginKey));
        }
        return ao;
    }
}
