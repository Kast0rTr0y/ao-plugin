package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;

/**
 * Factory to create instances of {@link com.atlassian.activeobjects.external.ActiveObjects}.
 */
public interface ActiveObjectsFactory
{
    /**
     * Tells whether the give data source type is supported by this factory, users should call this method before
     * calling {@link #create(ActiveObjectsConfiguration)} to avoid an {@link IllegalStateException} being thrown.
     *
     * @param configuration the configuration of active objects
     * @return {@code true} if the {@link ActiveObjectsConfiguration configuration} is supported.
     */
    boolean accept(ActiveObjectsConfiguration configuration);


    /**
     * Creates a <em>new</em> instance of {@link com.atlassian.activeobjects.external.ActiveObjects} each time it is called.
     *
     * @param configuration th configuration of active objects
     * @return the new {@link com.atlassian.activeobjects.external.ActiveObjects}
     * @throws IllegalStateException is the type of configuration is not supported by this factory
     * @see #accept(ActiveObjectsConfiguration)
     */
    ActiveObjects create(ActiveObjectsConfiguration configuration);
}
