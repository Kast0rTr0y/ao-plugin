package com.atlassian.activeobjects.config.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.activeobjects.internal.DataSourceTypeResolver;
import com.atlassian.activeobjects.config.PluginKey;
import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.RawEntity;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.NameConverters;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

/**
 * <p>Default implementation of the {@link com.atlassian.activeobjects.config.ActiveObjectsConfiguration}.</p>
 * <p>Note: it implements {@link #hashCode()} and {@link #equals(Object)} correctly to be used safely with collections. Those
 * implementation are based solely on the {@link com.atlassian.activeobjects.config.PluginKey} and nothing else as this is
 * the only immutable field.</p>
 */
public final class DefaultActiveObjectsConfiguration implements ActiveObjectsConfiguration
{
    private final PluginKey pluginKey;
    private final DataSourceTypeResolver dataSourceTypeResolver;
    private Prefix tableNamePrefix;
    private NameConverters nameConverters;
    private SchemaConfiguration schemaConfiguration;

    private Set<Class<? extends RawEntity<?>>> entities;
    private List<ActiveObjectsUpgradeTask> upgradeTasks;

    public DefaultActiveObjectsConfiguration(PluginKey pluginKey, DataSourceTypeResolver dataSourceTypeResolver)
    {
        this.pluginKey = checkNotNull(pluginKey);
        this.dataSourceTypeResolver = checkNotNull(dataSourceTypeResolver);
    }

    @Override
    public PluginKey getPluginKey()
    {
        return pluginKey;
    }

    @Override
    public DataSourceType getDataSourceType()
    {
        return dataSourceTypeResolver.getDataSourceType(getTableNamePrefix());
    }

    @Override
    public Prefix getTableNamePrefix()
    {
        return tableNamePrefix;
    }

    public void setTableNamePrefix(Prefix tableNamePrefix)
    {
        this.tableNamePrefix = tableNamePrefix;
    }

    @Override
    public NameConverters getNameConverters()
    {
        return nameConverters;
    }

    public void setNameConverters(NameConverters nameConverters)
    {
        this.nameConverters = nameConverters;
    }

    @Override
    public SchemaConfiguration getSchemaConfiguration()
    {
        return schemaConfiguration;
    }

    public void setSchemaConfiguration(SchemaConfiguration schemaConfiguration)
    {
        this.schemaConfiguration = schemaConfiguration;
    }

    @Override
    public Set<Class<? extends RawEntity<?>>> getEntities()
    {
        return entities;
    }

    public void setEntities(Set<Class<? extends RawEntity<?>>> entities)
    {
        this.entities = entities;
    }

    @Override
    public List<ActiveObjectsUpgradeTask> getUpgradeTasks()
    {
        return upgradeTasks;
    }

    public void setUpgradeTasks(List<ActiveObjectsUpgradeTask> upgradeTasks)
    {
        this.upgradeTasks = upgradeTasks;
    }

    @Override
    public final int hashCode()
    {
        return new HashCodeBuilder(5, 13).append(pluginKey).toHashCode();
    }

    @Override
    public final boolean equals(Object o)
    {
        if (o == null)
        {
            return false;
        }
        if (o == this)
        {
            return true;
        }
        if (o.getClass() != getClass())
        {
            return false;
        }

        final DefaultActiveObjectsConfiguration configuration = (DefaultActiveObjectsConfiguration) o;
        return new EqualsBuilder().append(pluginKey, configuration.pluginKey).isEquals();
    }
}
