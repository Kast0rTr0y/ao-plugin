package com.atlassian.activeobjects.config;

import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.activeobjects.internal.PluginKey;
import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.RawEntity;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.TableNameConverter;

import java.util.List;
import java.util.Set;

/** <p>This represents the configuration of active objects for a given module descriptor.</p> */
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
     * Gets the table name converter to use with Active Objects
     *
     * @return a TableNameConverter
     */
    TableNameConverter getTableNameConverter();

    /**
     * Gets the field name converter to use with Active Objects
     *
     * @return a field name converter
     */
    FieldNameConverter getFieldNameConverter();

    /**
     * Gets the schema configuration to use with Active Objects
     *
     * @return a schema configruation
     */
    SchemaConfiguration getSchemaConfiguration();

    /**
     * The set of 'configured' entitites for the active objects configuration.
     *
     * @return a set of entity classes, empty of no entities have been defined.
     */
    Set<Class<? extends RawEntity<?>>> getEntities();

    /**
     * Gets the upgrade tasks associated with Active Objects
     *
     * @return the upgrade tasks
     */
    List<ActiveObjectsUpgradeTask> getUpgradeTasks();
}
