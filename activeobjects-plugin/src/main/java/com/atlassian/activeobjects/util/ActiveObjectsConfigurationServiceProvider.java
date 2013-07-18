package com.atlassian.activeobjects.util;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;

import org.osgi.framework.Bundle;

/**
 * Interface to await the availability of an ActiveObjectsConfiguration service for a
 * given bundle.  ActiveObjectConfigurations are provided by plugins that consume the activeObjects module.
 * This interface assists in resolving that circular dependency.
 */
public interface ActiveObjectsConfigurationServiceProvider
{
    /**
     * Await the availability of an ActiveObjectsConfiguration for the given bundle.  This call blocks until
     * an ActiveObjectsConfiguration is available or the timeout has elapsed.
     *  
     * The ActiveObjectsConfiguration is currently optional, so in normal operation a service may not ever
     * become available.  Classpath scanning of known packages is performed if the configuration service is not found.
     * This method will always wait timeMs before attempting classpath scanning. 
     *  
     * @param bundle - the bundle to get the ActiveObjectsConfiguration for
     * @param timeMs - the amount of time to wait for the ActiveObjectsConfiguration service to become available
     * @return the ActiveObjectsConfiguration service for the given bundle
     * @throws InterruptedException
     */
    public ActiveObjectsConfiguration getAndWait(Bundle bundle, long timeMs) throws InterruptedException;
}
