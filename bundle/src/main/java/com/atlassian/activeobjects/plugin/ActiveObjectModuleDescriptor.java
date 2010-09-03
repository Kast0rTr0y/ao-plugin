package com.atlassian.activeobjects.plugin;

import com.atlassian.activeobjects.ActiveObjectsPluginException;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.activeobjects.internal.DataSourceTypeResolver;
import com.atlassian.activeobjects.internal.PluginKey;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.SimplePrefix;
import com.atlassian.activeobjects.osgi.OsgiServiceUtils;
import com.atlassian.activeobjects.util.Digester;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.util.validation.ValidationPattern;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import net.java.ao.RawEntity;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.dom4j.Element;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

/**
 * <p>The module descriptor for active objects.</p>
 * <p>This parses the 'ao' module definition and registers a 'bundle specific'
 * {@link com.atlassian.activeobjects.config.ActiveObjectsConfiguration configuration}
 * as an OSGi service.</p>
 * <p>This configuration is then looked up when the active object service is requested by the given bundle
 * through a &lt;component-import ... &gt; module to configure the service appropriately.</p>
 */
public final class ActiveObjectModuleDescriptor extends AbstractModuleDescriptor<Object>
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Easy registration of service
     */
    private final OsgiServiceUtils osgiUtils;

    private final Digester digester;

    private final DataSourceTypeResolver dataSourceTypeResolver;

    /**
     * The service registration for the active objects configuration, defined by this plugin.
     */
    private ServiceRegistration activeObjectsConfigurationServiceRegistration;

    public ActiveObjectModuleDescriptor(OsgiServiceUtils osgiUtils,
                                        DataSourceTypeResolver dataSourceTypeResolver, Digester digester)
    {
        this.osgiUtils = checkNotNull(osgiUtils);
        this.dataSourceTypeResolver = checkNotNull(dataSourceTypeResolver);
        this.digester = checkNotNull(digester);
    }

    @Override
    protected final void provideValidationRules(ValidationPattern pattern)
    {
        // make sure we validate the default
        super.provideValidationRules(pattern);

        // custom rules
        // ..
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        activeObjectsConfigurationServiceRegistration = osgiUtils.registerService(getBundle(), ActiveObjectsConfiguration.class, getActiveObjectsBundleConfiguration(element));
    }

    @Override
    public void disabled()
    {
        if (activeObjectsConfigurationServiceRegistration != null)
        {
            activeObjectsConfigurationServiceRegistration.unregister();
        }
        super.disabled();
    }

    @Override
    public Object getModule()
    {
        return null; // no module
    }

    private ActiveObjectsConfiguration getActiveObjectsBundleConfiguration(Element element)
    {
        final DefaultActiveObjectsConfiguration configuration =
                new DefaultActiveObjectsConfiguration(PluginKey.fromBundle(getBundle()), dataSourceTypeResolver);

        configuration.setTableNamePrefix(getTableNamePrefix(element));
        configuration.setEntities(getEntities(element));
        return configuration;
    }

    private Prefix getTableNamePrefix(Element element)
    {
        return new SimplePrefix("ao_" + digester.digest(getNameSpace(element), 6), "_");
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
        private Set<Class<? extends RawEntity<?>>> entities;

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
            return dataSourceTypeResolver.getDataSourceType(pluginKey);
        }

        public Prefix getTableNamePrefix()
        {
            return tableNamePrefix;
        }

        public void setTableNamePrefix(Prefix tableNamePrefix)
        {
            this.tableNamePrefix = tableNamePrefix;
        }

        public Set<Class<? extends RawEntity<?>>> getEntities()
        {
            return entities;
        }

        public void setEntities(Set<Class<? extends RawEntity<?>>> entities)
        {
            this.entities = entities;
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
