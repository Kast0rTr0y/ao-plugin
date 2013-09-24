package com.atlassian.activeobjects.util;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.osgi.NoServicesFoundException;
import com.atlassian.plugin.PluginException;

import org.osgi.framework.Bundle;

import java.util.concurrent.TimeUnit;

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
     * become available.  Classpath scanning of known packages is performed only if the configuration service is not found.
     * This method will always wait up to waitTime for the configuration service before attempting classpath scanning. 
     *  
     * @param bundle - the bundle to get the ActiveObjectsConfiguration for
     * @param waitTime - the amount of time to wait for the ActiveObjectsConfiguration service to become available
     * @param unit - the unit of waitTime
     * @return the ActiveObjectsConfiguration service for the given bundle, cannot be null
     * @throws NoServicesFoundException if no configuration OSGi service is found and no classes were found scanning the well known packages.
     */
    public ActiveObjectsConfiguration getAndWait(Bundle bundle, long waitTime, TimeUnit unit) throws InterruptedException, NoServicesFoundException;
    
    /**
     * If this method returns true getAndWait will not need to wait for an AOConfiguration to become available
     * @param bundle - the bundle to check configuration for
     * @return true if there is configuration currently present for the bundle.
     */
    public boolean hasConfiguration(Bundle bundle);
}
