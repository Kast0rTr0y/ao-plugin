package com.atlassian.activeobjects.internal;

import com.atlassian.sal.api.ApplicationProperties;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 *
 */
public class DefaultActiveObjectsFactoryResolver implements ActiveObjectsFactoryResolver
{
    private final ApplicationProperties applicationProperties;
    private final DatabaseConfiguration configuration;

    public DefaultActiveObjectsFactoryResolver(ApplicationProperties applicationProperties, DatabaseConfiguration configuration)
    {
        this.applicationProperties = checkNotNull(applicationProperties);
        this.configuration = checkNotNull(configuration);
    }

    public ActiveObjectsFactory get(String pluginKey)
    {
        return new DatabaseDirectoryAwareActiveObjectsFactory(pluginKey, applicationProperties, configuration);
    }
}
