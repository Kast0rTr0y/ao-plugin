package com.atlassian.activeobjects.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.internal.util.MultiServiceTracker;

import java.util.Map;
import java.util.Arrays;

/**
 * Starts the active objects services
 */
public class ActiveObjectsActivator implements BundleActivator
{
    private final Logger log = LoggerFactory.getLogger(ActiveObjectsActivator.class);
    private final ActiveObjectsServiceFactory serviceFactory = new ActiveObjectsServiceFactory();

    MultiServiceTracker depTracker;
    BundleContext bundleContext;

    public void start(final BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
        bundleContext.registerService(ActiveObjects.class.getName(), serviceFactory, null);

        depTracker = new MultiServiceTracker(new MultiServiceTracker.MultiServiceTrackerCustomizer()
        {
            public void refresh(Map<Class<?>, Object> dependencies) {
                init(dependencies);
            }

            public void unregister() {
                serviceFactory.stop();
            }
        }, bundleContext,
                Arrays.<Class<?>>asList(ApplicationProperties.class),
                Arrays.<Class<?>>asList(ActiveObjectsConfiguration.class));
    }

    public void stop(BundleContext bundleContext) throws Exception {
        depTracker.close();
    }

    private void init(Map<Class<?>,Object> deps)
    {
        log.debug("Initializing active object services");
        ActiveObjectsConfiguration config = (ActiveObjectsConfiguration) deps.get(ActiveObjectsConfiguration.class);
        if (config == null)
        {
            config = new DefaultActiveObjectsConfiguration();
        }
        DefaultActiveObjectsProvider prov = new DefaultActiveObjectsProvider(
                (ApplicationProperties) deps.get(ApplicationProperties.class), config);

        serviceFactory.init(prov);
    }
}
