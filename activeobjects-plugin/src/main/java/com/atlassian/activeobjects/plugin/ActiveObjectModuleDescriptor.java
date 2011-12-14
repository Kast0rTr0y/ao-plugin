package com.atlassian.activeobjects.plugin;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.admin.PluginToTablesMapping;
import com.atlassian.activeobjects.EntitiesValidator;
import com.atlassian.activeobjects.ao.PrefixedSchemaConfiguration;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.activeobjects.internal.DataSourceTypeResolver;
import com.atlassian.activeobjects.internal.PluginKey;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.SimplePrefix;
import com.atlassian.activeobjects.internal.config.NameConvertersFactory;
import com.atlassian.activeobjects.osgi.OsgiServiceUtils;
import com.atlassian.activeobjects.util.Digester;
import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.java.ao.RawEntity;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.TableNameConverter;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.dom4j.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.atlassian.activeobjects.admin.PluginToTablesMapping.*;
import static com.atlassian.activeobjects.ao.ConverterUtils.toUpperCase;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.newLinkedList;

/**
 * <p>The module descriptor for active objects.</p>
 * <p>This parses the 'ao' module definition and registers a 'bundle specific'
 * {@link com.atlassian.activeobjects.config.ActiveObjectsConfiguration configuration}
 * as an OSGi service.</p>
 * <p>This configuration is then looked up when the active object service is requested by the given bundle
 * through a &lt;component-import ... &gt; module to configure the service appropriately.</p>
 */
public class ActiveObjectModuleDescriptor extends AbstractModuleDescriptor<Object>
{
    public static final String AO_TABLE_PREFIX = "AO";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final OsgiServiceUtils osgiUtils;
    private final Digester digester;
    private final DataSourceTypeResolver dataSourceTypeResolver;
    private final NameConvertersFactory nameConvertersFactory;
    private final EntitiesValidator entitiesValidator;
    private final PluginToTablesMapping pluginToTablesMapping;

    private String hash;
    private Prefix tableNamePrefix;
    private NameConverters nameConverters;

    private Set<Class<? extends RawEntity<?>>> entityClasses;

    /**
     * The service registration for the active objects configuration, defined by this plugin.
     */
    private ServiceRegistration activeObjectsConfigurationServiceRegistration;
    private ServiceRegistration tableNameConverterServiceRegistration;
    private List<ActiveObjectsUpgradeTask> upgradeTasks;

    public ActiveObjectModuleDescriptor(OsgiServiceUtils osgiUtils,
                                        DataSourceTypeResolver dataSourceTypeResolver,
                                        Digester digester,
                                        NameConvertersFactory nameConvertersFactory,
                                        PluginToTablesMapping pluginToTablesMapping,
                                        EntitiesValidator entitiesValidator)
    {
        this.osgiUtils = checkNotNull(osgiUtils);
        this.dataSourceTypeResolver = checkNotNull(dataSourceTypeResolver);
        this.digester = checkNotNull(digester);
        this.pluginToTablesMapping = checkNotNull(pluginToTablesMapping);
        this.nameConvertersFactory = checkNotNull(nameConvertersFactory);
        this.entitiesValidator = checkNotNull(entitiesValidator);
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);

        tableNamePrefix = getTableNamePrefix(element);
        nameConverters = nameConvertersFactory.getNameConverters(tableNamePrefix);
        entityClasses = entitiesValidator.check(getEntities(element), nameConverters);
        upgradeTasks = getUpgradeTasks(element);

        recordTables(entityClasses, nameConverters.getTableNameConverter());
    }

    public String getHash()
    {
        return hash;
    }

    private List<ActiveObjectsUpgradeTask> getUpgradeTasks(Element element)
    {
        final List<Element> upgradeTask = getSubElements(element, "upgradeTask");

        final List<Class<ActiveObjectsUpgradeTask>> classes = Lists.transform(upgradeTask, new Function<Element, Class<ActiveObjectsUpgradeTask>>()
        {
            @Override
            public Class<ActiveObjectsUpgradeTask> apply(Element utElement)
            {
                final String upgradeTaskClass = utElement.getText().trim();
                logger.debug("Found upgrade task class <{}>", upgradeTaskClass);
                return getUpgradeTaskClass(upgradeTaskClass);
            }
        });

        final AutowireCapablePlugin plugin = (AutowireCapablePlugin) getPlugin();
        return Lists.transform(classes, new Function<Class<ActiveObjectsUpgradeTask>, ActiveObjectsUpgradeTask>()
        {
            @Override
            public ActiveObjectsUpgradeTask apply(Class<ActiveObjectsUpgradeTask> upgradeTaskClass)
            {
                return plugin.autowire(upgradeTaskClass);
            }
        });
    }

    private Class<ActiveObjectsUpgradeTask> getUpgradeTaskClass(String upgradeTask)
    {
        try
        {
            return getPlugin().loadClass(upgradeTask, getClass());
        }
        catch (ClassNotFoundException e)
        {
            throw new ActiveObjectsPluginException(e);
        }
    }

    void recordTables(Set<Class<? extends RawEntity<?>>> entityClasses, final TableNameConverter tableNameConverter)
    {
        pluginToTablesMapping.add(PluginInfo.of(getPlugin()), Lists.transform(newLinkedList(entityClasses), new Function<Class<? extends RawEntity<?>>, String>()
        {
            @Override
            public String apply(Class<? extends RawEntity<?>> from)
            {
                return tableNameConverter.getName(from);
            }
        }));
    }

    @Override
    public void enabled()
    {
        super.enabled();

        if (tableNameConverterServiceRegistration == null)
        {
            tableNameConverterServiceRegistration = osgiUtils.registerService(getBundle(), TableNameConverter.class, nameConverters.getTableNameConverter());
        }
        if (activeObjectsConfigurationServiceRegistration == null)
        {
            activeObjectsConfigurationServiceRegistration = osgiUtils.registerService(getBundle(), ActiveObjectsConfiguration.class, getActiveObjectsBundleConfiguration(tableNamePrefix, nameConverters, entityClasses));
        }
    }

    @Override
    public void disabled()
    {
        unregister(activeObjectsConfigurationServiceRegistration);
        activeObjectsConfigurationServiceRegistration = null;
        unregister(tableNameConverterServiceRegistration);
        tableNameConverterServiceRegistration = null;
        super.disabled();
    }

    @Override
    public Object getModule()
    {
        return null; // no module
    }

    private ActiveObjectsConfiguration getActiveObjectsBundleConfiguration(Prefix tableNamePrefix, NameConverters nameConverters, Set<Class<? extends RawEntity<?>>> entities)
    {
        final DefaultActiveObjectsConfiguration configuration =
                new DefaultActiveObjectsConfiguration(PluginKey.fromBundle(getBundle()), dataSourceTypeResolver);

        configuration.setTableNamePrefix(tableNamePrefix);
        configuration.setNameConverters(nameConverters);
        configuration.setSchemaConfiguration(new PrefixedSchemaConfiguration(tableNamePrefix));
        configuration.setEntities(entities);
        configuration.setUpgradeTasks(upgradeTasks);
        return configuration;
    }

    private void unregister(ServiceRegistration serviceRegistration)
    {
        if (serviceRegistration != null)
        {
            try
            {
                serviceRegistration.unregister();
            }
            catch (IllegalStateException ignored)
            {
                logger.debug("Service has already been unregistered", ignored);
            }
        }
    }

    private Prefix getTableNamePrefix(Element element)
    {
        hash = digester.digest(getNameSpace(element), 6);
        return new SimplePrefix(toUpperCase(AO_TABLE_PREFIX + "_" + hash), "_");
    }

    /**
     * The table name space is either the custom namespace set by the product, or the bundle symbolic name
     *
     * @param element the 'ao' descriptor element
     * @return the name space for names
     */
    private String getNameSpace(Element element)
    {
        final String custom = element.attributeValue("namespace");
        return custom != null ? custom : getBundle().getSymbolicName();
    }

    private Set<Class<? extends RawEntity<?>>> getEntities(Element element)
    {
        return Sets.newHashSet(Iterables.transform(getEntityClassNames(element), new Function<String, Class<? extends RawEntity<?>>>()
        {
            public Class<? extends RawEntity<?>> apply(String entityClassName)
            {
                return getEntityClass(entityClassName);
            }
        }));
    }

    private Class<? extends RawEntity<?>> getEntityClass(String entityClassName)
    {
        try
        {
            return getPlugin().loadClass(entityClassName, getClass());
        }
        catch (ClassNotFoundException e)
        {
            throw new ActiveObjectsPluginException(e);
        }
    }

    private Iterable<String> getEntityClassNames(Element element)
    {
        return Iterables.transform(getSubElements(element, "entity"), new Function<Element, String>()
        {
            public String apply(Element entityElement)
            {
                final String entityClassName = entityElement.getText().trim();
                logger.debug("Found entity class <{}>", entityClassName);
                return entityClassName;
            }
        });
    }

    private Bundle getBundle()
    {
        return ((OsgiPlugin) getPlugin()).getBundle();
    }

    @SuppressWarnings("unchecked")
    private static List<Element> getSubElements(Element element, String name)
    {
        return element.elements(name);
    }

    /**
     * <p>Default implementation of the {@link com.atlassian.activeobjects.config.ActiveObjectsConfiguration}.</p>
     * <p>Note: it implements {@link #hashCode()} and {@link #equals(Object)} correctly to be used safely with collections. Those
     * implementation are based solely on the {@link com.atlassian.activeobjects.internal.PluginKey} and nothing else as this is
     * the only immutable field.</p>
     */
    private static class DefaultActiveObjectsConfiguration implements ActiveObjectsConfiguration
    {
        private final PluginKey pluginKey;
        private final DataSourceTypeResolver dataSourceTypeResolver;
        private Prefix tableNamePrefix;
        private NameConverters nameConverters;
        private SchemaConfiguration schemaConfiguration;
        private Set<Class<? extends RawEntity<?>>> entities;
        private List<ActiveObjectsUpgradeTask> upgradeTasks;

        DefaultActiveObjectsConfiguration(PluginKey pluginKey, DataSourceTypeResolver dataSourceTypeResolver)
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
}
