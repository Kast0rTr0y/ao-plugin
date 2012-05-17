package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.activeobjects.internal.PluginKey;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.PluginException;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;
import net.java.ao.RawEntity;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.NameConverters;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

/**
 * <p>This is the service factory that will create the {@link com.atlassian.activeobjects.external.ActiveObjects}
 * instance for each plugin using active objects.</p>
 *
 * <p>The instance created by that factory is a delegating instance that works together with the
 * {@link ActiveObjectsServiceFactory} to get a correctly configure instance according
 * to the {@link com.atlassian.activeobjects.config.ActiveObjectsConfiguration plugin configuration} and
 * the application configuration.</p>
 */
public final class ActiveObjectsServiceFactory implements ServiceFactory
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    final Map<ActiveObjectsKey, DelegatingActiveObjects> aoInstances = new MapMaker().makeComputingMap(new Function<ActiveObjectsKey, DelegatingActiveObjects>()
    {
        @Override
        public DelegatingActiveObjects apply(final ActiveObjectsKey key)
        {
            return new DelegatingActiveObjects(new Supplier<ActiveObjects>()
            {
                @Override
                public ActiveObjects get()
                {
                    return createActiveObjects(key.bundle);
                }
            });
        }
    });

    private final OsgiServiceUtils osgiUtils;
    private final ActiveObjectsFactory factory;

    public ActiveObjectsServiceFactory(OsgiServiceUtils osgiUtils, ActiveObjectsFactory factory, EventPublisher eventPublisher)
    {
        this.osgiUtils = checkNotNull(osgiUtils);
        this.factory = checkNotNull(factory);
        checkNotNull(eventPublisher).register(this);
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        return aoInstances.get(new ActiveObjectsKey(bundle));
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object ao)
    {
        try
        {
            final ActiveObjects removed = aoInstances.remove(new ActiveObjectsKey(bundle));
            if (removed != null)
            {
                checkState(ao == removed);

                //we can't flush cache because some dependencies may have been de-registered already.
                //removed.flushAll(); // clear all caches for good measure
            }
            else
            {
                logger.warn("Didn't find Active Objects instance matching {}, this shouldn't be happening!", bundle);
            }
        }
        catch (Exception e)
        {
            throw new ActiveObjectsPluginException("An exception occurred un-getting the AO service for bundle " + bundle + ". This could lead to memory leaks!", e);
        }
    }

    /**
     * Listens for {@link HotRestartEvent} and releases all {@link ActiveObjects instances} flushing their caches.
     * @param hotRestartEvent
     */
    @EventListener
    public void onHotRestart(HotRestartEvent hotRestartEvent)
    {
        for (DelegatingActiveObjects ao : ImmutableList.copyOf(aoInstances.values()))
        {
            ao.removeDelegate();
        }
    }

    /**
     * Creates a delegating active objects that will lazily create the properly configured active objects.
     *
     * @param bundle the bundle for which to create the {@link com.atlassian.activeobjects.external.ActiveObjects}
     * @return an {@link com.atlassian.activeobjects.external.ActiveObjects} instance
     */
    private ActiveObjects createActiveObjects(Bundle bundle)
    {
        logger.debug("Creating active object service for bundle {} [{}]", bundle.getSymbolicName(), bundle.getBundleId());
        return factory.create(new LazyActiveObjectConfiguration(bundle));
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

    private static final class ActiveObjectsKey
    {
        public final Bundle bundle;

        private ActiveObjectsKey(Bundle bundle)
        {
            this.bundle = checkNotNull(bundle);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            final ActiveObjectsKey that = (ActiveObjectsKey) o;

            return this.bundle.getBundleId() == that.bundle.getBundleId();
        }

        @Override
        public int hashCode()
        {
            return ((Long) bundle.getBundleId()).hashCode();
        }
    }

    final class LazyActiveObjectConfiguration implements ActiveObjectsConfiguration
    {
        private final Bundle bundle;

        public LazyActiveObjectConfiguration(Bundle bundle)
        {
            this.bundle = checkNotNull(bundle);
        }

        @Override
        public PluginKey getPluginKey()
        {
            return getDelegate().getPluginKey();
        }

        @Override
        public DataSourceType getDataSourceType()
        {
            return getDelegate().getDataSourceType();
        }

        @Override
        public Prefix getTableNamePrefix()
        {
            return getDelegate().getTableNamePrefix();
        }

        @Override
        public NameConverters getNameConverters()
        {
            return getDelegate().getNameConverters();
        }

        @Override
        public SchemaConfiguration getSchemaConfiguration()
        {
            return getDelegate().getSchemaConfiguration();
        }

        @Override
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
