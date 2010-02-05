package com.atlassian.activeobjects.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * <p>This is the service factory that will create the {@link com.atlassian.activeobjects.external.ActiveObjects} instance for each plugin
 * using active objects.</p>
 * <p>The instance created by that factory is a delegating instance that works together with the {@link com.atlassian.activeobjects.internal.ActiveObjectsProvider}
 * to get a correctly configure instance according to the plugin configuration and the application configuration.</p>
 */
public class ActiveObjectsServiceFactory implements ServiceFactory
{
    private final ActiveObjectsProvider provider;
    private final PluginKeyFactory keyFactory;

    public ActiveObjectsServiceFactory(ActiveObjectsProvider provider, PluginKeyFactory keyFactory)
    {
        this.provider = checkNotNull(provider);
        this.keyFactory = checkNotNull(keyFactory);
    }

    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        return new DelegatingActiveObjects(keyFactory.get(bundle), provider);
    }

    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object o)
    {
    }
}
