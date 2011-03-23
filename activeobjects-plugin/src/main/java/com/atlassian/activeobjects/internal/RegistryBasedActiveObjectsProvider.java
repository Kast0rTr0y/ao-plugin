package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;

public final class RegistryBasedActiveObjectsProvider implements ActiveObjectsProvider
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ActiveObjectsRegistry registry;
    private final ActiveObjectsFactory activeObjectsFactory;

    public RegistryBasedActiveObjectsProvider(ActiveObjectsRegistry registry, ActiveObjectsFactory activeObjectsFactory)
    {
        this.registry = checkNotNull(registry);
        this.activeObjectsFactory = checkNotNull(activeObjectsFactory);
    }

    public synchronized ActiveObjects get(ActiveObjectsConfiguration configuration)
    {
        final ActiveObjects ao = registry.get(configuration);
        if (ao == null) // we need to create one
        {
            logger.debug("Could not find existing {} service for configuration {}, creating a new one", ActiveObjects.class.getName(), configuration);
            return registry.register(configuration, activeObjectsFactory.create(configuration));
        }
        else
        {
            logger.debug("Found existing {} service for configuration {}", ActiveObjects.class.getName(), configuration);
            return ao;
        }
    }
}
