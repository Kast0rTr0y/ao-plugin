package com.atlassian.activeobjects.config;

import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.activeobjects.internal.PluginKey;
import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.RawEntity;
import net.java.ao.schema.TableNameConverter;

import java.util.Set;

/**
 * <p>This represents the configuration of active objects for a given module descriptor.</p>
 */
public interface ActiveObjectsConfiguration
{
    /**
     * The plugin key for which this configuration is defined.
     *
     * @return a {@link com.atlassian.activeobjects.internal.PluginKey}, cannot be {@link null}
     */
    PluginKey getPluginKey();

    /**
     * The datasource type that this active objects is meant to use.
     *
     * @return a valid DataSourceType
     */
    DataSourceType getDataSourceType();

    /**
     * The prefix to use for table names in the database
     *
     * @return the prefix to use for table names in the database.
     */
    Prefix getTableNamePrefix();

    /**
     * The set of 'configured' entitites for the active objects configuration.
     *
     * @return a set of entity classes.
     */
    Set<Class<? extends RawEntity<?>>> getEntities();
}
