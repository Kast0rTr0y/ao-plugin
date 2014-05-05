package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.config.ActiveObjectsConfigurationFactory;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import net.java.ao.RawEntity;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;

public class AOConfigurationServiceProviderImpl implements AOConfigurationGenerator
{
    private static Logger log = LoggerFactory.getLogger(AOConfigurationServiceProviderImpl.class);

    private final ActiveObjectsConfigurationFactory aoConfigurationFactory;
    private final ApplicationContext applicationContext;

    public AOConfigurationServiceProviderImpl(ActiveObjectsConfigurationFactory aoConfigurationFactory, ApplicationContext applicationContext)
    {
        this.aoConfigurationFactory = checkNotNull(aoConfigurationFactory);
        this.applicationContext = checkNotNull(applicationContext);
    }

    @Override
    public ActiveObjectsConfiguration generateScannedConfiguration(@Nonnull final Bundle bundle, @Nonnull final String entityPackage)
    {
        checkNotNull(bundle);
        checkNotNull(entityPackage);

        final Set<Class<? extends RawEntity<?>>> entities = scanEntities(bundle, entityPackage);
        if (!entities.isEmpty())
        {
            log.debug("bundle [{}] generating configuration for scanned entity classes {}", bundle.getSymbolicName(), entities);

            return aoConfigurationFactory.getConfiguration(bundle, bundle.getSymbolicName(), entities,
                    scanUpgradeTask(bundle, entityPackage));
        }
        else
        {
            return null;
        }
    }

    private List<ActiveObjectsUpgradeTask> scanUpgradeTask(final Bundle bundle, final String entityPackage)
    {
        final BundleContext bundleContext = bundle.getBundleContext();

        // not typing the iterable here, because of the cast afterward, which wouldn't compile otherwise!
        final Iterable upgradeClasses = new BundleContextScanner().findClasses(bundleContext, entityPackage,
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

    private Set<Class<? extends RawEntity<?>>> scanEntities(Bundle bundle, final String entityPackage)
    {
        final BundleContext bundleContext = bundle.getBundleContext();

        // not typing the iterable here, because of the cast afterward, which wouldn't compile otherwise!
        final Iterable entityClasses = new BundleContextScanner().findClasses(bundleContext, entityPackage,
                new LoadClassFromBundleFunction(bundleContext.getBundle()), new IsAoEntityPredicate());

        @SuppressWarnings("unchecked")
        // we're filtering to get what we want!
        final Iterable<Class<? extends RawEntity<?>>> entities = (Iterable<Class<? extends RawEntity<?>>>) entityClasses;
        return ImmutableSet.copyOf(entities);
    }
}
