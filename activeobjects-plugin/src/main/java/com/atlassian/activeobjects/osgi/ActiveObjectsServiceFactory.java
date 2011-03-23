package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.internal.ActiveObjectsProvider;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.activeobjects.internal.PluginKey;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.plugin.PluginException;
import net.java.ao.RawEntity;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.TableNameConverter;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

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

    private final OsgiServiceUtils osgiUtils;
    private final ActiveObjectsProvider provider;

    public ActiveObjectsServiceFactory(OsgiServiceUtils osgiUtils, ActiveObjectsProvider provider)
    {
        this.osgiUtils = checkNotNull(osgiUtils);
        this.provider = checkNotNull(provider);
    }

    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        return createActiveObjects(bundle);
    }

    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object ao)
    {
        // no-op
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
        return new DelegatingActiveObjects(new LazyActiveObjectConfiguration(bundle), provider);
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
            return osgiUtils.getService(bundle, ActiveObjectsConfiguration.class);
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

    final class LazyActiveObjectConfiguration implements ActiveObjectsConfiguration
    {
        private final Bundle bundle;

        public LazyActiveObjectConfiguration(Bundle bundle)
        {
            this.bundle = checkNotNull(bundle);
        }

        public PluginKey getPluginKey()
        {
            return getDelegate().getPluginKey();
        }

        public DataSourceType getDataSourceType()
        {
            return getDelegate().getDataSourceType();
        }

        public Prefix getTableNamePrefix()
        {
            return getDelegate().getTableNamePrefix();
        }

        public TableNameConverter getTableNameConverter()
        {
            return getDelegate().getTableNameConverter();
        }

        public FieldNameConverter getFieldNameConverter()
        {
            return getDelegate().getFieldNameConverter();
        }

        public SchemaConfiguration getSchemaConfiguration()
        {
            return getDelegate().getSchemaConfiguration();
        }

        public Set<Class<? extends RawEntity<?>>> getEntities()
        {
            return getDelegate().getEntities();
        }

        @Override
        public List<ActiveObjectsUpgradeTask> getUpgradeTasks()
        {
            return getDelegate().getUpgradeTasks();
        }

        @Override
        public int hashCode()
        {
            return getDelegate().hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj != null
                    && obj instanceof LazyActiveObjectConfiguration
                    && bundle.getBundleId() == ((LazyActiveObjectConfiguration) obj).bundle.getBundleId();
        }
        
        ActiveObjectsConfiguration getDelegate()
        {
            return getConfiguration(bundle);
        }
    }
}
