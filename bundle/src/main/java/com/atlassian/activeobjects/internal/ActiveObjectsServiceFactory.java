package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.backup.ActiveObjectsBackupFactory;
import com.atlassian.sal.api.backup.BackupRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * <p>This is the service factory that will create the {@link com.atlassian.activeobjects.external.ActiveObjects}
 * instance for each plugin using active objects.</p>
 *
 * <p>The instance created by that factory is a delegating instance that works together with the
 * {@link com.atlassian.activeobjects.internal.ActiveObjectsProvider} to get a correctly configure instance according
 * to the plugin configuration and the application configuration.</p>
 */
public final class ActiveObjectsServiceFactory implements ServiceFactory
{
    private final ActiveObjectsProvider provider;
    private final BackupRegistry backupRegistry;
    private final ActiveObjectsBackupFactory backupFactory;

    public ActiveObjectsServiceFactory(ActiveObjectsProvider provider, BackupRegistry backupRegistry, ActiveObjectsBackupFactory backupFactory)
    {
        this.provider = checkNotNull(provider);
        this.backupRegistry = backupRegistry;
        this.backupFactory = checkNotNull(backupFactory);
    }

    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        final ActiveObjects ao = createActiveObjects(bundle);
        registerForBackup(bundle, ao);
        return ao;
    }

    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object ao)
    {
        unregisterForbackup(bundle, ao);
    }

    private DelegatingActiveObjects createActiveObjects(Bundle bundle)
    {
        return new DelegatingActiveObjects(PluginKey.fromBundle(bundle), provider);
    }

    private void registerForBackup(Bundle bundle, ActiveObjects ao)
    {
        backupRegistry.register(backupFactory.getBackup(bundle, ao));
    }

    private void unregisterForbackup(Bundle bundle, Object ao)
    {
        backupRegistry.unregister(backupFactory.getBackup(bundle, (ActiveObjects) ao));
    }
}
