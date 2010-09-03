package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsProvider;
import com.atlassian.activeobjects.internal.backup.ActiveObjectsBackupFactory;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.plugin.PluginException;
import com.atlassian.sal.api.backup.BackupRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

/**
 * <p>This is the service factory that will create the {@link com.atlassian.activeobjects.external.ActiveObjects}
 * instance for each plugin using active objects.</p>
 *
 * <p>The instance created by that factory is a delegating instance that works together with the
 * {@link com.atlassian.activeobjects.internal.ActiveObjectsProvider} to get a correctly configure instance according
 * to the {@link com.atlassian.activeobjects.config.ActiveObjectsConfiguration plugin configuration} and
 * the application configuration.</p>
 */
public final class ActiveObjectsServiceFactory implements ServiceFactory
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ActiveObjectOsgiServiceUtils<ActiveObjectsConfiguration> osgiUtils;
    private final ActiveObjectsProvider provider;

    private final BackupRegistry backupRegistry;
    private final ActiveObjectsBackupFactory backupFactory;

    public ActiveObjectsServiceFactory(ActiveObjectOsgiServiceUtils<ActiveObjectsConfiguration> osgiUtils, ActiveObjectsProvider provider, BackupRegistry backupRegistry, ActiveObjectsBackupFactory backupFactory)
    {
        this.osgiUtils = checkNotNull(osgiUtils);
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

    /**
     * Creates a delegating active objects that will lazily create the properly configured active objects.
     *
     * @param bundle the bundle for which to create the {@link com.atlassian.activeobjects.external.ActiveObjects}
     * @return an {@link com.atlassian.activeobjects.external.ActiveObjects} instance
     */
    private ActiveObjects createActiveObjects(Bundle bundle)
    {
        logger.debug("Creating active object service for bundle {}", bundle.getSymbolicName());
        return new DelegatingActiveObjects(getConfiguration(bundle), provider);
    }

    /**
     * Retrieves the active objects configuration which should be exposed as a service.
     *
     * @param bundle the bundle for which to find the active objects configuration.
     * @return the found {@link com.atlassian.activeobjects.config.ActiveObjectsConfiguration}, can't be {@code null}
     * @throws PluginException is 0 or more than one configuration is found.
     */
    private ActiveObjectsConfiguration getConfiguration(Bundle bundle)
    {
        try
        {
            return osgiUtils.getService(bundle);
        }
        catch (TooManyServicesFoundException e)
        {
            logger.error("Found multiple active objects configurations for bundle " + bundle.getSymbolicName() + ". Only one active objects module descriptor (ao) allowed per plugin!");
            throw new PluginException(e);
        }
        catch (NoServicesFoundException e)
        {
            logger.error("Could not find any active objects configurations for bundle " + bundle.getSymbolicName() + ".\n" +
                    "Did you define an 'ao' module descriptor in your plugin?\n" +
                    "Try adding this in your atlassian-plugin.xml file: <ao key='some-key' />");
            throw new PluginException(e);
        }
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
