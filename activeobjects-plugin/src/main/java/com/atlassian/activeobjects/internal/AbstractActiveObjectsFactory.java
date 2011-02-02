package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import net.java.ao.RawEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

/**
 * Abstract implementation of {@link com.atlassian.activeobjects.internal.ActiveObjectsFactory} that implements the
 * basic contract for a single {@link com.atlassian.activeobjects.internal.DataSourceType}.
 */
abstract class AbstractActiveObjectsFactory implements ActiveObjectsFactory
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DataSourceType supportedDataSourceType;

    AbstractActiveObjectsFactory(DataSourceType dataSourceType)
    {
        this.supportedDataSourceType = checkNotNull(dataSourceType);
    }

    public final boolean accept(ActiveObjectsConfiguration configuration)
    {
        return supportedDataSourceType.equals(configuration.getDataSourceType());
    }

    public final ActiveObjects create(ActiveObjectsConfiguration configuration)
    {
        if (!accept(configuration))
        {
            throw new IllegalStateException(configuration + " is not supported. Did you can #accept(ActiveObjectConfiguration) before calling me?");
        }
        final ActiveObjects ao = doCreate(configuration);
        final Set<Class<? extends RawEntity<?>>> entitiesToMigrate = configuration.getEntities();
        logger.debug("Created active objects instance with configuration {}, now migrating entities {}", configuration, entitiesToMigrate);
        ao.migrate(asArray(entitiesToMigrate));
        return ao;
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
    protected abstract ActiveObjects doCreate(ActiveObjectsConfiguration configuration);
}
