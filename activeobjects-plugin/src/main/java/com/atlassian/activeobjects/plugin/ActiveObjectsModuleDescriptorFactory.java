package com.atlassian.activeobjects.plugin;

import com.atlassian.activeobjects.admin.PluginToTablesMapping;
import com.atlassian.activeobjects.EntitiesValidator;
import com.atlassian.activeobjects.internal.DataSourceTypeResolver;
import com.atlassian.activeobjects.internal.config.NameConvertersFactory;
import com.atlassian.activeobjects.osgi.OsgiServiceUtils;
import com.atlassian.activeobjects.util.Digester;
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
    private final DataSourceTypeResolver dataSourceTypeResolver;
    private final Digester digester;
    private final NameConvertersFactory nameConvertersFactory;
    private final PluginToTablesMapping pluginToTablesMapping;
    private final EntitiesValidator entitiesValidator;

    public ActiveObjectsModuleDescriptorFactory(HostContainer hostContainer, OsgiServiceUtils osgiUtils,
                                                DataSourceTypeResolver dataSourceTypeResolver, Digester digester,
                                                NameConvertersFactory nameConvertersFactory, PluginToTablesMapping pluginToTablesMapping,
                                                EntitiesValidator entitiesValidator)
    {
        super(checkNotNull(hostContainer), "ao", ActiveObjectModuleDescriptor.class);
        this.osgiUtils = checkNotNull(osgiUtils);
        this.dataSourceTypeResolver = checkNotNull(dataSourceTypeResolver);
        this.digester = checkNotNull(digester);
        this.pluginToTablesMapping = checkNotNull(pluginToTablesMapping);
        this.nameConvertersFactory = checkNotNull(nameConvertersFactory);
        this.entitiesValidator = checkNotNull(entitiesValidator);
    }

    @Override
    public ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        return hasModuleDescriptor(type) ? new ActiveObjectModuleDescriptor(osgiUtils, dataSourceTypeResolver, digester, nameConvertersFactory, pluginToTablesMapping, entitiesValidator) : null;
    }
}
