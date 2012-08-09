package com.atlassian.activeobjects.config.internal;

import com.atlassian.activeobjects.ao.PrefixedSchemaConfiguration;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.config.ActiveObjectsConfigurationFactory;
import com.atlassian.activeobjects.config.PluginKey;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.internal.DataSourceTypeResolver;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.SimplePrefix;
import com.atlassian.activeobjects.internal.config.NameConvertersFactory;
import com.atlassian.activeobjects.util.Digester;
import net.java.ao.RawEntity;
import net.java.ao.schema.NameConverters;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Bundle;

import java.util.List;
import java.util.Set;

import static com.atlassian.activeobjects.ao.ConverterUtils.toUpperCase;
import static com.google.common.base.Preconditions.checkNotNull;

public final class DefaultActiveObjectsConfigurationFactory implements ActiveObjectsConfigurationFactory
{
    private final Digester digester;
    private final NameConvertersFactory nameConvertersFactory;
    private final DataSourceTypeResolver dataSourceTypeResolver;

    public DefaultActiveObjectsConfigurationFactory(Digester digester, NameConvertersFactory nameConvertersFactory, DataSourceTypeResolver dataSourceTypeResolver)
    {
        this.digester = checkNotNull(digester);
        this.nameConvertersFactory = checkNotNull(nameConvertersFactory);
        this.dataSourceTypeResolver = checkNotNull(dataSourceTypeResolver);
    }

    @Override
    public ActiveObjectsConfiguration getConfiguration(Bundle bundle, String namespace, Set<Class<? extends RawEntity<?>>> entities, List<ActiveObjectsUpgradeTask> upgradeTasks)
    {
        final PluginKey pluginKey = PluginKey.fromBundle(bundle);
        final Prefix tableNamePrefix = getTableNamePrefix(bundle, namespace);
        final NameConverters nameConverters = nameConvertersFactory.getNameConverters(tableNamePrefix);

        final DefaultActiveObjectsConfiguration defaultActiveObjectsConfiguration = new DefaultActiveObjectsConfiguration(pluginKey, dataSourceTypeResolver);
        defaultActiveObjectsConfiguration.setTableNamePrefix(tableNamePrefix);
        defaultActiveObjectsConfiguration.setNameConverters(nameConverters);
        defaultActiveObjectsConfiguration.setSchemaConfiguration(new PrefixedSchemaConfiguration(tableNamePrefix));

        defaultActiveObjectsConfiguration.setEntities(entities);
        defaultActiveObjectsConfiguration.setUpgradeTasks(upgradeTasks);

        return defaultActiveObjectsConfiguration;
    }

    private Prefix getTableNamePrefix(Bundle bundle, String namespace)
    {
        return getTableNamePrefix(StringUtils.isNotBlank(namespace) ? namespace : bundle.getSymbolicName());
    }

    private Prefix getTableNamePrefix(String namespace)
    {
        final String hash = digester.digest(namespace, 6);
        return new SimplePrefix(toUpperCase(ActiveObjectsConfiguration.AO_TABLE_PREFIX + "_" + hash), "_");
    }
}
