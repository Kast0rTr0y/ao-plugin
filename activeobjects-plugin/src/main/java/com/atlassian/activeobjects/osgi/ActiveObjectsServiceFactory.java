package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.config.ActiveObjectsConfigurationFactory;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.activeobjects.config.PluginKey;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.PluginException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import net.java.ao.RawEntity;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.NameConverters;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;

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
    private final ActiveObjectsConfigurationFactory configurationFactory;
    private final ApplicationContext applicationContext;

    public ActiveObjectsServiceFactory(ApplicationContext applicationContext, OsgiServiceUtils osgiUtils, ActiveObjectsFactory factory, ActiveObjectsConfigurationFactory configurationFactory, EventPublisher eventPublisher)
    {
        this.applicationContext = checkNotNull(applicationContext);
        this.osgiUtils = checkNotNull(osgiUtils);
        this.factory = checkNotNull(factory);
        this.configurationFactory = checkNotNull(configurationFactory);
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
     * Retrieves the active objects configuration which should be exposed as a service or if none is found will scan for
     * well known packages for entity classes and upgrade classes to create an appropriate configuration.
     *
     * @param bundle the bundle for which to find the active objects configuration.
     * @return the found {@link com.atlassian.activeobjects.config.ActiveObjectsConfiguration}, can't be {@code null}
     * @throws PluginException if no configuration OSGi service is found and no classes were found scanning the well known packages.
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
            logger.debug("Didn't find any active objects configuration service for bundle " + bundle.getSymbolicName() + ".  Will scan for AO classes in default packages of bundle.");

            final Set<Class<? extends RawEntity<?>>> entities = scanEntities(bundle);
            if (!entities.isEmpty())
            {
                return  configurationFactory.getConfiguration(bundle, bundle.getSymbolicName(), entities, scanUpgradeTask(bundle), null);
            }
            else
            {
                final String msg = "Didn't find any configuration service for bundle " + bundle.getSymbolicName() + " nor any entities scanning for default AO packages.";
                logger.error(msg);
                throw new PluginException(msg, e);
            }
        }
    }

    private Set<Class<? extends RawEntity<?>>> scanEntities(Bundle bundle)
    {
        final BundleContext bundleContext = bundle.getBundleContext();

        // not typing the iterable here, because of the cast afterward, which wouldn't compile otherwise!
        final Iterable entityClasses =
                new BundleContextScanner().findClasses(
                        bundleContext,
                        "ao.model",
                        new LoadClassFromBundleFunction(bundleContext.getBundle()),
                        new IsAoEntityPredicate()
                );

        @SuppressWarnings("unchecked") // we're filtering to get what we want!
        final Iterable<Class<? extends RawEntity<?>>> entities = (Iterable<Class<? extends RawEntity<?>>>) entityClasses;
        return ImmutableSet.copyOf(entities);
    }

    private List<ActiveObjectsUpgradeTask> scanUpgradeTask(Bundle bundle)
    {
        final BundleContext bundleContext = bundle.getBundleContext();

        // not typing the iterable here, because of the cast afterward, which wouldn't compile otherwise!
        final Iterable upgradeClasses =
                new BundleContextScanner().findClasses(
                        bundleContext,
                        "ao.upgrade",
                        new LoadClassFromBundleFunction(bundleContext.getBundle()),
                        new IsAoUpgradeTaskPredicate()
                );

        @SuppressWarnings("unchecked") // we're filtering to get what we want!
        final Iterable<Class<? extends ActiveObjectsUpgradeTask>> upgrades = (Iterable<Class<? extends ActiveObjectsUpgradeTask>>) upgradeClasses;

        return copyOf(transform(upgrades, new Function<Class<? extends ActiveObjectsUpgradeTask>, ActiveObjectsUpgradeTask>()
        {
            @Override
            public ActiveObjectsUpgradeTask apply(Class<? extends ActiveObjectsUpgradeTask> input)
            {
                return (ActiveObjectsUpgradeTask) applicationContext.getAutowireCapableBeanFactory()
                        .createBean(input, AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT, true);
            }
        }));
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

    private static class IsAoEntityPredicate implements Predicate<Class>
    {
        @Override
        public boolean apply(Class clazz)
        {
            return RawEntity.class.isAssignableFrom(clazz);
        }
    }

    private static class IsAoUpgradeTaskPredicate implements Predicate<Class>
    {
        @Override
        public boolean apply(Class clazz)
        {
            return ActiveObjectsUpgradeTask.class.isAssignableFrom(clazz);
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
        public void validate()
        {
            getDelegate().validate();
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
