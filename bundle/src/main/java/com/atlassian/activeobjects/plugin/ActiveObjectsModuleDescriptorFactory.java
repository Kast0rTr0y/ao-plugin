package com.atlassian.activeobjects.plugin;

import com.atlassian.activeobjects.internal.config.ActiveObjectsBundleConfiguration;
import com.atlassian.activeobjects.osgi.ActiveObjectOsgiServiceUtils;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * The factory to create the &lt;ao ...&gt; module descriptor.
 */
public final class ActiveObjectsModuleDescriptorFactory extends SingleModuleDescriptorFactory<ActiveObjectModuleDescriptor>
{
    private final ActiveObjectOsgiServiceUtils<ActiveObjectsBundleConfiguration> osgiUtils;

    public ActiveObjectsModuleDescriptorFactory(HostContainer hostContainer, ActiveObjectOsgiServiceUtils<ActiveObjectsBundleConfiguration> osgiUtils)
    {
        super(checkNotNull(hostContainer), "ao", ActiveObjectModuleDescriptor.class);
        this.osgiUtils = checkNotNull(osgiUtils);
    }

    @Override
    public ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        return hasModuleDescriptor(type) ? new ActiveObjectModuleDescriptor(osgiUtils) : null;
    }
}
