package com.atlassian.activeobjects.util;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.osgi.NoServicesFoundException;

import org.osgi.framework.Bundle;

/**
 * Interface to await the availability of an ActiveObjectsConfiguration service for a
 * given bundle.  ActiveObjectConfigurations are provided by plugins that consume the activeObjects module.
 * This interface assists in resolving that circular dependency.
 */
public interface ActiveObjectsConfigurationServiceProvider
{
    /**
     * Await the availability of an OSGI ActiveObjectsConfiguration service for the given bundle.
     *
     * The ActiveObjectsConfiguration is currently optional, so in normal operation a service may not ever
     * become available. Classpath scanning of known packages is performed only if the configuration service is not
     * found after a fixed timeout.
     *  
     * @param bundle - the bundle to get the ActiveObjectsConfiguration for
     * @return the ActiveObjectsConfiguration service for the given bundle, cannot be null
     * @throws NoServicesFoundException if no configuration OSGi service is found and no classes were found scanning the well known packages.
     */
    public ActiveObjectsConfiguration getAndWait(Bundle bundle) throws InterruptedException, NoServicesFoundException;
}
