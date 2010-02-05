package com.atlassian.activeobjects.internal;

/**
 * Resolver that will give access to the correct {@link com.atlassian.activeobjects.internal.ActiveObjectsFactory} given a plugin key.
 * Getting the correct factory might be driven, for example, by the configuration of the plugin.
 */
public interface ActiveObjectsFactoryResolver
{
    /**
     * Gets the {@link com.atlassian.activeobjects.internal.ActiveObjectsFactory} associated with the plugin referenced by {@code pluginKey}
     * @param pluginKey the key of the plugin
     * @return the 'correct' non-null factory
     */
    ActiveObjectsFactory get(String pluginKey);
}
