package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.google.common.base.Supplier;

import net.java.ao.RawEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract implementation of {@link com.atlassian.activeobjects.internal.ActiveObjectsFactory} that implements the
 * basic contract for a single {@link com.atlassian.activeobjects.internal.DataSourceType}.
 */
abstract class AbstractActiveObjectsFactory implements ActiveObjectsFactory
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DataSourceType supportedDataSourceType;
    private final ActiveObjectUpgradeManager aoUpgradeManager;

    AbstractActiveObjectsFactory(DataSourceType dataSourceType, ActiveObjectUpgradeManager aoUpgradeManager)
    {
        this.supportedDataSourceType = checkNotNull(dataSourceType);
        this.aoUpgradeManager = checkNotNull(aoUpgradeManager);
    }

    @Override
    public final boolean accept(ActiveObjectsConfiguration configuration)
    {
        return supportedDataSourceType.equals(configuration.getDataSourceType());
    }

    @Override
    public final ActiveObjects create(final ActiveObjectsConfiguration configuration, DatabaseType dbType)
    {
        if (!accept(configuration))
        {
            throw new IllegalStateException(configuration + " is not supported. Did you can #accept(ActiveObjectConfiguration) before calling me?");
        }

        upgrade(configuration, dbType);

        final ActiveObjects ao = doCreate(configuration, dbType);

        final Set<Class<? extends RawEntity<?>>> entitiesToMigrate = configuration.getEntities();
        logger.debug("Created active objects instance with configuration {}, now migrating entities {}", configuration, entitiesToMigrate);
        ao.migrate(asArray(entitiesToMigrate));
        return ao;
    }

    private void upgrade(final ActiveObjectsConfiguration configuration, final DatabaseType dbType)
    {
        aoUpgradeManager.upgrade(configuration.getTableNamePrefix(), configuration.getUpgradeTasks(), new Supplier<ActiveObjects>()
            {
                @Override
                public ActiveObjects get()
                {
                    return doCreate(configuration, dbType);
                }
            });
    }

    @SuppressWarnings("unchecked")
    private Class<? extends RawEntity<?>>[] asArray(Collection<Class<? extends RawEntity<?>>> classes)
    {
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * This has the same contract as {@link #create(ActiveObjectsConfiguration)} except that checking the configuration
     * type has already been taken care of.
     *
     * @param configuration the configuration to work with
     * @return the new {@link com.atlassian.activeobjects.external.ActiveObjects}
     */
    protected abstract ActiveObjects doCreate(ActiveObjectsConfiguration configuration, DatabaseType dbType);
}
