package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.backup.BackupRegistry;
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
    private final BackupRegistry backupRegistry;
    private final ActiveObjectsBackupFactory backupFactory;

    public ActiveObjectsServiceFactory(ActiveObjectsProvider provider, PluginKeyFactory keyFactory, BackupRegistry backupRegistry, ActiveObjectsBackupFactory backupFactory)
    {
        this.provider = checkNotNull(provider);
        this.keyFactory = checkNotNull(keyFactory);
        this.backupRegistry = checkNotNull(backupRegistry);
        this.backupFactory = checkNotNull(backupFactory);
    }

    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        final ActiveObjects ao = new DelegatingActiveObjects(keyFactory.get(bundle), provider);
        backupRegistry.register(backupFactory.getBackup(bundle, ao));
        return ao;
    }

    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object ao)
    {
        backupRegistry.unregister(backupFactory.getBackup(bundle, (ActiveObjects) ao));
    }
}
