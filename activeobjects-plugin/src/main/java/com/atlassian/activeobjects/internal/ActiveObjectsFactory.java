package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.tenancy.api.Tenant;

/**
 * Factory to create instances of {@link com.atlassian.activeobjects.external.ActiveObjects}.
 */
public interface ActiveObjectsFactory {
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
     * @param tenant        against which to create
     * @return the new {@link com.atlassian.activeobjects.external.ActiveObjects}
     * @throws IllegalStateException                                           is the type of configuration is not supported by this factory
     * @throws com.atlassian.activeobjects.internal.ActiveObjectsInitException on failure to lock across the cluster prior to upgradation
     * @see #accept(ActiveObjectsConfiguration)
     */
    ActiveObjects create(ActiveObjectsConfiguration configuration, Tenant tenant);
}
