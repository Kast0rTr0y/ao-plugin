package com.atlassian.activeobjects.plugin;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.ao.ActiveObjectsFieldNameConverter;
import com.atlassian.activeobjects.ao.ActiveObjectsTableNameConverter;
import com.atlassian.activeobjects.ao.PrefixedSchemaConfiguration;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.activeobjects.internal.DataSourceTypeResolver;
import com.atlassian.activeobjects.internal.PluginKey;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.SimplePrefix;
import com.atlassian.activeobjects.osgi.OsgiServiceUtils;
import com.atlassian.activeobjects.util.Digester;
import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.util.validation.ValidationPattern;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.java.ao.RawEntity;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.FieldNameConverter;
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

import static com.atlassian.activeobjects.ao.ConverterUtils.*;
import static com.google.common.base.Preconditions.*;

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

    private static final int MAX_NUMBER_OF_ENTITIES = 50;
    private static final int MAX_LENGTH_ENTITY_NAME = 30;

    /**
     * Easy registration of service
     */
    private final OsgiServiceUtils osgiUtils;

    private final Digester digester;

    private final DataSourceTypeResolver dataSourceTypeResolver;

    private String hash;
    private Prefix tableNamePrefix;
    private TableNameConverter tableNameConverter;
    private FieldNameConverter fieldNameConverter;

    private Set<Class<? extends RawEntity<?>>> entityClasses;

    /**
     * The service registration for the active objects configuration, defined by this plugin.
     */
    private ServiceRegistration activeObjectsConfigurationServiceRegistration;
    private ServiceRegistration tableNameConverterServiceRegistration;
    private List<ActiveObjectsUpgradeTask> upgradeTasks;

    public ActiveObjectModuleDescriptor(OsgiServiceUtils osgiUtils,
                                        DataSourceTypeResolver dataSourceTypeResolver,
                                        Digester digester)
    {
        this.osgiUtils = checkNotNull(osgiUtils);
        this.dataSourceTypeResolver = checkNotNull(dataSourceTypeResolver);
        this.digester = checkNotNull(digester);
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);

        tableNamePrefix = getTableNamePrefix(element);
        tableNameConverter = new ActiveObjectsTableNameConverter(tableNamePrefix);
        fieldNameConverter = new ActiveObjectsFieldNameConverter();
        entityClasses = getEntities(element);
        upgradeTasks = getUpgradeTasks(element);

        validateEntities(entityClasses, tableNameConverter);
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

    void validateEntities(Set<Class<? extends RawEntity<?>>> entityClasses, TableNameConverter tableNameConverter)
    {
        if (entityClasses.size() > MAX_NUMBER_OF_ENTITIES)
        {
            throw new PluginException("Plugins are allowed no more than " + MAX_NUMBER_OF_ENTITIES + " entities!");
        }

        for (Class<? extends RawEntity<?>> entityClass : entityClasses)
        {
            final String tableName = tableNameConverter.getName(entityClass);
            if (tableName.length() > MAX_LENGTH_ENTITY_NAME)
            {
                logger.error("Invalid entity defined in AO module of plugin, {}", getPluginKey());
                logger.error("Table names cannot be longer than 30 chars long in order to work with Oracle. " +
                        "Entity class <{}> gets a generated table name of <{}> with is beyond the 30 chars long limit.", entityClass.getName(), tableName);
                logger.error("Please rename this entity to get a shorter table name.");

                throw new PluginException("Invalid entity in AO descriptor of plugin " + getPluginKey() + ", generated table name is too long! Should be no more than 30 chars.");
            }
        }
    }

    @Override
    public void enabled()
    {
        super.enabled();

        if (tableNameConverterServiceRegistration == null)
        {
            tableNameConverterServiceRegistration = osgiUtils.registerService(getBundle(), TableNameConverter.class, tableNameConverter);
        }
        if (activeObjectsConfigurationServiceRegistration == null)
        {
            activeObjectsConfigurationServiceRegistration = osgiUtils.registerService(getBundle(), ActiveObjectsConfiguration.class, getActiveObjectsBundleConfiguration(tableNamePrefix, tableNameConverter, fieldNameConverter, entityClasses));
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

    private ActiveObjectsConfiguration getActiveObjectsBundleConfiguration(Prefix tableNamePrefix, TableNameConverter tableNameConverter, FieldNameConverter fieldNameConverter, Set<Class<? extends RawEntity<?>>> entities)
    {
        final DefaultActiveObjectsConfiguration configuration =
                new DefaultActiveObjectsConfiguration(PluginKey.fromBundle(getBundle()), dataSourceTypeResolver);

        configuration.setTableNamePrefix(tableNamePrefix);
        configuration.setTableNameConverter(tableNameConverter);
        configuration.setFieldNameConverter(fieldNameConverter);
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
        private TableNameConverter tableNameConverter;
        private FieldNameConverter fieldNameConverter;
        private SchemaConfiguration schemaConfiguration;
        private Set<Class<? extends RawEntity<?>>> entities;
        private List<ActiveObjectsUpgradeTask> upgradeTasks;

        public DefaultActiveObjectsConfiguration(PluginKey pluginKey, DataSourceTypeResolver dataSourceTypeResolver)
        {
            this.pluginKey = checkNotNull(pluginKey);
            this.dataSourceTypeResolver = checkNotNull(dataSourceTypeResolver);
        }

        public PluginKey getPluginKey()
        {
            return pluginKey;
        }

        public DataSourceType getDataSourceType()
        {
            return dataSourceTypeResolver.getDataSourceType(getTableNamePrefix());
        }

        public Prefix getTableNamePrefix()
        {
            return tableNamePrefix;
        }

        public void setTableNamePrefix(Prefix tableNamePrefix)
        {
            this.tableNamePrefix = tableNamePrefix;
        }

        public TableNameConverter getTableNameConverter()
        {
            return tableNameConverter;
        }

        public void setTableNameConverter(TableNameConverter tableNameConverter)
        {
            this.tableNameConverter = tableNameConverter;
        }

        public FieldNameConverter getFieldNameConverter()
        {
            return fieldNameConverter;
        }

        public void setFieldNameConverter(FieldNameConverter fieldNameConverter)
        {
            this.fieldNameConverter = fieldNameConverter;
        }

        public SchemaConfiguration getSchemaConfiguration()
        {
            return schemaConfiguration;
        }

        public void setSchemaConfiguration(SchemaConfiguration schemaConfiguration)
        {
            this.schemaConfiguration = schemaConfiguration;
        }

        public Set<Class<? extends RawEntity<?>>> getEntities()
        {
            return entities;
        }

        public void setEntities(Set<Class<? extends RawEntity<?>>> entities)
        {
            this.entities = entities;
        }

        public List<ActiveObjectsUpgradeTask> getUpgradeTasks()
        {
            return upgradeTasks;
        }

        public void setUpgradeTasks(List<ActiveObjectsUpgradeTask> upgradeTasks)
        {
            this.upgradeTasks = upgradeTasks;
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(5, 13).append(pluginKey).toHashCode();
        }

        @Override
        public boolean equals(Object o)
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
