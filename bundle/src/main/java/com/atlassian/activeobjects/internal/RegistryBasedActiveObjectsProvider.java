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
    private final DataSourceTypeResolver dataSourceTypeResolver;

    public RegistryBasedActiveObjectsProvider(ActiveObjectsRegistry registry, ActiveObjectsFactoryResolver resolver, DataSourceTypeResolver dataSourceTypeResolver)
    {
        this.registry = checkNotNull(registry);
        this.resolver = checkNotNull(resolver);
        this.dataSourceTypeResolver = checkNotNull(dataSourceTypeResolver);
    }

    public synchronized ActiveObjects get(PluginKey pluginKey)
    {
        ActiveObjects ao = registry.get(pluginKey);
        if (ao == null) // we need to create one
        {
            ao = registry.register(pluginKey, resolver.get(dataSourceTypeResolver.getDataSourceType(pluginKey)).create(pluginKey));
        }
        return ao;
    }
}
