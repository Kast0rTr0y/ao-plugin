package com.atlassian.activeobjects.osgi;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.java.ao.RawEntity;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.osgi.context.BundleContextAware;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.config.ActiveObjectsConfigurationFactory;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.util.ActiveObjectsConfigurationServiceProvider;
import com.atlassian.plugin.PluginException;
import com.atlassian.util.concurrent.BlockingReference;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

public class AOConfigurationServiceProviderImpl implements ActiveObjectsConfigurationServiceProvider, BundleContextAware, InitializingBean
{
    private static Logger log = LoggerFactory.getLogger(AOConfigurationServiceProviderImpl.class);

    private BlockingReferenceMap<Long, Boolean> bundleKeyToAOConfigurationGate = new BlockingReferenceMap<Long, Boolean>();

    private BundleContext bundleContext;
    private final OsgiServiceUtils osgiUtils;
    private final ActiveObjectsConfigurationFactory aoConfigurationFactory;
    private final ApplicationContext applicationContext;

    public AOConfigurationServiceProviderImpl(OsgiServiceUtils osgiUtils,
            ActiveObjectsConfigurationFactory aoConfigurationFactory, ApplicationContext applicationContext)
    {
        this.osgiUtils = checkNotNull(osgiUtils);
        this.aoConfigurationFactory = checkNotNull(aoConfigurationFactory);
        this.applicationContext = checkNotNull(applicationContext);
    }

    // TODO implement a BundleContextFactoryBean, similar to ApplicationContextFactoryBean
    @Override
    public void setBundleContext(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        checkNotNull(bundleContext);
        initServiceListener();
    }

    public ActiveObjectsConfiguration getAndWait(Bundle bundle, long timeMs) throws InterruptedException
    {
        try
        {
            bundleKeyToAOConfigurationGate.getAndWait(bundle.getBundleId(), timeMs);
            return checkNotNull(getConfiguration(bundle));
        }
        catch (TimeoutException e)
        {
            log.warn("Timeout ({}ms) waiting for ActiveObjectConfiguration for Bundle : {}.  To avoid this warning add an ao configuration module to your plugin", bundle, timeMs);
            log.debug("Stacktrace: ", e);
            // Since the configuration object is optional we cannot just thruw a timeout, attempt classpath scanning
            ActiveObjectsConfiguration configuration = createConfigurationByScanningEntities(bundle);
            if (configuration == null)
            {
                final String msg = "Timed out waiting for configuration service for bundle " + bundle.getSymbolicName() +
                        " and didn't find any entities scanning for default AO packages.";
                log.error(msg);
                throw new PluginException(msg, e);
            }
            else
            {
                return configuration;
            }
        }

    }

    /**
     * Retrieves the active objects configuration which should be exposed as a service or if none is found will scan for
     * well known packages for entity classes and upgrade classes to create an appropriate configuration.
     * 
     * @param bundle
     *            the bundle for which to find the active objects configuration.
     * @return the found {@link com.atlassian.activeobjects.config.ActiveObjectsConfiguration}, can't be {@code null}
     * @throws PluginException
     *             if no configuration OSGi service is found and no classes were found scanning the well known packages.
     */
    private ActiveObjectsConfiguration getConfiguration(Bundle bundle)
    {
        try
        {
            return osgiUtils.getService(bundle, ActiveObjectsConfiguration.class);
        }
        catch (TooManyServicesFoundException e)
        {
            log.error("Found multiple active objects configurations for bundle " + bundle.getSymbolicName() +
                    ". Only one active objects module descriptor (ao) allowed per plugin!");
            throw new PluginException(e);
        }
        catch (NoServicesFoundException e)
        {
            ActiveObjectsConfiguration configuration = createConfigurationByScanningEntities(bundle);
            if (configuration == null)
            {
                final String msg = "Didn't find any configuration service for bundle " + bundle.getSymbolicName() +
                        " nor any entities scanning for default AO packages.";
                log.error(msg);
                throw new PluginException(msg, e);
            }
            else
            {
                return configuration;
            }
        }
    }
    
    private ActiveObjectsConfiguration createConfigurationByScanningEntities(Bundle bundle)
    {
        log.debug("Didn't find any active objects configuration service for bundle " + bundle.getSymbolicName() +
                ".  Will scan for AO classes in default packages of bundle.");

        final Set<Class<? extends RawEntity<?>>> entities = scanEntities(bundle);
        if (!entities.isEmpty())
        {
            return aoConfigurationFactory.getConfiguration(bundle, bundle.getSymbolicName(), entities,
                    scanUpgradeTask(bundle));
        }
        else
        {
            return null;
        }        
    }

    private List<ActiveObjectsUpgradeTask> scanUpgradeTask(Bundle bundle)
    {
        final BundleContext bundleContext = bundle.getBundleContext();

        // not typing the iterable here, because of the cast afterward, which wouldn't compile otherwise!
        final Iterable upgradeClasses = new BundleContextScanner().findClasses(bundleContext, "ao.upgrade",
                new LoadClassFromBundleFunction(bundleContext.getBundle()), new IsAoUpgradeTaskPredicate());

        @SuppressWarnings("unchecked")
        // we're filtering to get what we want!
        final Iterable<Class<? extends ActiveObjectsUpgradeTask>> upgrades = (Iterable<Class<? extends ActiveObjectsUpgradeTask>>) upgradeClasses;

        return copyOf(transform(upgrades,
                new Function<Class<? extends ActiveObjectsUpgradeTask>, ActiveObjectsUpgradeTask>()
                {
                    @Override
                    public ActiveObjectsUpgradeTask apply(Class<? extends ActiveObjectsUpgradeTask> input)
                    {
                        return (ActiveObjectsUpgradeTask) applicationContext.getAutowireCapableBeanFactory()
                                .createBean(input, AutowireCapableBeanFactory.AUTOWIRE_AUTODETECT, true);
                    }
                }));
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

    private Set<Class<? extends RawEntity<?>>> scanEntities(Bundle bundle)
    {
        final BundleContext bundleContext = bundle.getBundleContext();

        // not typing the iterable here, because of the cast afterward, which wouldn't compile otherwise!
        final Iterable entityClasses = new BundleContextScanner().findClasses(bundleContext, "ao.model",
                new LoadClassFromBundleFunction(bundleContext.getBundle()), new IsAoEntityPredicate());

        @SuppressWarnings("unchecked")
        // we're filtering to get what we want!
        final Iterable<Class<? extends RawEntity<?>>> entities = (Iterable<Class<? extends RawEntity<?>>>) entityClasses;
        return ImmutableSet.copyOf(entities);
    }

    private void initServiceListener()
    {
        String filter = "(objectclass=" + ActiveObjectsConfiguration.class.getName() + ")";
        ServiceListener serviceListener = new ServiceListener()
        {
            @Override
            public void serviceChanged(ServiceEvent event)
            {
                final ServiceReference serviceReference = event.getServiceReference();
                switch (event.getType())
                {
                case ServiceEvent.REGISTERED:
                    bundleKeyToAOConfigurationGate.putIfAbsent(serviceReference.getBundle().getBundleId(), Boolean.TRUE);
                    break;

                case ServiceEvent.UNREGISTERING:
                    bundleKeyToAOConfigurationGate.remove(serviceReference.getBundle().getBundleId());
                    break;
                    
                default:
                    break;
                }
            }
        };
        try
        {
            bundleContext.addServiceListener(serviceListener, filter);
            ServiceReference[] serviceReferences = bundleContext.getServiceReferences(null, filter);
            if (serviceReferences != null)
            {
                for (ServiceReference reference : bundleContext.getServiceReferences(null, filter))
                {
                    serviceListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
                }
            }
        }
        catch (InvalidSyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class BlockingReferenceMap<K, V>
    {
        private ConcurrentHashMap<K, BlockingReference<V>> innerConcurrentMap = new ConcurrentHashMap<K, BlockingReference<V>>();

        public V getAndWait(K key, long waitMs) throws TimeoutException, InterruptedException
        {
            return safeGetReference(key).get(waitMs, TimeUnit.MILLISECONDS);
        }

        public void putIfAbsent(K key, V config)
        {
            BlockingReference<V> reference = safeGetReference(key);
            checkArgument(reference.isEmpty());
            safeGetReference(key).set(config);
        }

        public void remove(K key)
        {
            safeGetReference(key).clear();
        }

        private BlockingReference<V> safeGetReference(K key)
        {
            BlockingReference<V> newReference = BlockingReference.newSRSW();
            BlockingReference<V> existingRef = innerConcurrentMap.putIfAbsent(key, newReference);
            if (existingRef != null)
                return existingRef;
            return newReference;
        }
    }
}
