package com.atlassian.activeobjects.plugin;

import com.atlassian.activeobjects.EntitiesValidator;
import com.atlassian.activeobjects.admin.PluginToTablesMapping;
import com.atlassian.activeobjects.config.ActiveObjectsConfigurationFactory;
import com.atlassian.activeobjects.osgi.OsgiServiceUtils;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;

import static com.google.common.base.Preconditions.*;

/**
 * The factory to create the &lt;ao ...&gt; module descriptor.
 */
public final class ActiveObjectsModuleDescriptorFactory extends SingleModuleDescriptorFactory<ActiveObjectModuleDescriptor>
{
    private final OsgiServiceUtils osgiUtils;
    private final PluginToTablesMapping pluginToTablesMapping;
    private final EntitiesValidator entitiesValidator;
    private final ActiveObjectsConfigurationFactory configurationFactory;

    public ActiveObjectsModuleDescriptorFactory(HostContainer hostContainer,
                                                ActiveObjectsConfigurationFactory configurationFactory,
                                                OsgiServiceUtils osgiUtils,
                                                PluginToTablesMapping pluginToTablesMapping,
                                                EntitiesValidator entitiesValidator)
    {
        super(checkNotNull(hostContainer), "ao", ActiveObjectModuleDescriptor.class);
        this.configurationFactory = checkNotNull(configurationFactory);
        this.osgiUtils = checkNotNull(osgiUtils);
        this.pluginToTablesMapping = checkNotNull(pluginToTablesMapping);
        this.entitiesValidator = checkNotNull(entitiesValidator);
    }

    @Override
    public ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        return hasModuleDescriptor(type) ? new ActiveObjectModuleDescriptor(configurationFactory, osgiUtils, pluginToTablesMapping, entitiesValidator) : null;
    }
}
