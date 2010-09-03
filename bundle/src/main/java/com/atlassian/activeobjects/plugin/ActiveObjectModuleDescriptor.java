package com.atlassian.activeobjects.plugin;

import com.atlassian.activeobjects.internal.config.ActiveObjectsBundleConfiguration;
import com.atlassian.activeobjects.osgi.ActiveObjectOsgiServiceUtils;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.util.validation.ValidationPattern;
import net.java.ao.RawEntity;
import org.dom4j.Element;
import org.osgi.framework.Bundle;

import java.util.Collections;
import java.util.Set;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * <p>The module descriptor for active objects.</p>
 * <p>This parses the 'ao' module definition and registers a 'bundle specific'
 * {@link com.atlassian.activeobjects.internal.config.ActiveObjectsBundleConfiguration configuration}
 * as an OSGi service.</p>
 * <p>This configuration is then looked up when the active object service is requested by the given bundle
 * through a &lt;component-import ... &gt; module to configure the service appropriately.</p>
 */
public final class ActiveObjectModuleDescriptor extends AbstractModuleDescriptor<Object>
{
    /**
     * Easy registration of service
     */
    private final ActiveObjectOsgiServiceUtils<ActiveObjectsBundleConfiguration> osgiUtils;

    public ActiveObjectModuleDescriptor(ActiveObjectOsgiServiceUtils<ActiveObjectsBundleConfiguration> osgiUtils)
    {
        this.osgiUtils = checkNotNull(osgiUtils);
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
        osgiUtils.registerService(getBundle(), getActiveObjectsBundleConfiguration());
    }

    @Override
    public Object getModule()
    {
        return null; // no module
    }

    private ActiveObjectsBundleConfiguration getActiveObjectsBundleConfiguration()
    {
        return new DefaultActiveObjectsBundleConfiguration();
    }

    private Bundle getBundle()
    {
        return ((OsgiPlugin) getPlugin()).getBundle();
    }

    private static class DefaultActiveObjectsBundleConfiguration implements ActiveObjectsBundleConfiguration
    {
        public Set<Class<? extends RawEntity<?>>> getEntities()
        {
            return Collections.emptySet();
        }
    }
}
