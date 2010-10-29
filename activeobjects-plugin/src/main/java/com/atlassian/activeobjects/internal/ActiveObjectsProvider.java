package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;

/**
 * <p>Provides a pooled instance of the {@link com.atlassian.activeobjects.external.ActiveObjects}.</p>
 * <p>Multiple calls for the same configuration will return the same {@link com.atlassian.activeobjects.external.ActiveObjects}
 * instance, as long as there is at least one strongly-held reference.</p>
 * <p>It is recommended clients use the {@link com.atlassian.activeobjects.external.ActiveObjects} service directly as
 * it will choose a consistent key automatically.</p>
 */
public interface ActiveObjectsProvider
{
    /**
     * Gets an {@link com.atlassian.activeobjects.external.ActiveObjects} instance per identifier. Instances are pooled
     * so multiple calls of the same identifier will return identical results.
     *
     * @param configuration the active object's configuration for a given plugin
     * @return the {@link com.atlassian.activeobjects.external.ActiveObjects} instance
     */
    ActiveObjects get(ActiveObjectsConfiguration configuration);
}