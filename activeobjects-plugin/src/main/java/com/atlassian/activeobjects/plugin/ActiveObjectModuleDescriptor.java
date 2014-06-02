package com.atlassian.activeobjects.plugin;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.EntitiesValidator;
import com.atlassian.activeobjects.admin.PluginToTablesMapping;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.config.ActiveObjectsConfigurationFactory;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.osgi.OsgiServiceUtils;
import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.java.ao.RawEntity;
import net.java.ao.schema.TableNameConverter;
import org.dom4j.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.atlassian.activeobjects.admin.PluginToTablesMapping.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

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
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ActiveObjectsConfigurationFactory configurationFactory;
    private final OsgiServiceUtils osgiUtils;
    private final EntitiesValidator entitiesValidator;
    private final PluginToTablesMapping pluginToTablesMapping;

    /**
     * The service registration for the active objects configuration, defined by this plugin.
     */
    private ServiceRegistration activeObjectsConfigurationServiceRegistration;
    private ServiceRegistration tableNameConverterServiceRegistration;

    private ActiveObjectsConfiguration configuration;

    public ActiveObjectModuleDescriptor(ModuleFactory moduleFactory,
                                        ActiveObjectsConfigurationFactory configurationFactory,
                                        OsgiServiceUtils osgiUtils,
                                        PluginToTablesMapping pluginToTablesMapping,
                                        EntitiesValidator entitiesValidator)
    {
        super(moduleFactory);
        this.configurationFactory = checkNotNull(configurationFactory);
        this.osgiUtils = checkNotNull(osgiUtils);
        this.pluginToTablesMapping = checkNotNull(pluginToTablesMapping);
        this.entitiesValidator = checkNotNull(entitiesValidator);
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);

        final Set<Class<? extends RawEntity<?>>> entities = getEntities(element);
        final List<ActiveObjectsUpgradeTask> upgradeTasks = getUpgradeTasks(element);
        configuration = getActiveObjectsConfiguration(getNameSpace(element), entities, upgradeTasks);
    }

    public void validate()
    {
        final Set<Class<? extends RawEntity<?>>> entityClasses = entitiesValidator.check(configuration.getEntities(), configuration.getNameConverters());
        recordTables(entityClasses, configuration.getNameConverters().getTableNameConverter());
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
            tableNameConverterServiceRegistration = osgiUtils.registerService(getBundle(), TableNameConverter.class, configuration.getNameConverters().getTableNameConverter());
        }
        if (activeObjectsConfigurationServiceRegistration == null)
        {
            activeObjectsConfigurationServiceRegistration = osgiUtils.registerService(getBundle(), ActiveObjectsConfiguration.class, configuration);
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

    public ActiveObjectsConfiguration getConfiguration()
    {
        return configuration;
    }

    private ActiveObjectsConfiguration getActiveObjectsConfiguration(String namespace, Set<Class<? extends RawEntity<?>>> entities, List<ActiveObjectsUpgradeTask> upgradeTasks)
    {
        return configurationFactory.getConfiguration(getBundle(), namespace, entities, upgradeTasks, this);
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
}
