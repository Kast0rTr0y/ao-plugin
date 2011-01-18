package com.atlassian.activeobjects.plugin;

import com.atlassian.activeobjects.internal.DataSourceTypeResolver;
import com.atlassian.activeobjects.osgi.OsgiServiceUtils;
import com.atlassian.activeobjects.util.Digester;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;
import net.java.ao.schema.TableNameConverter;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

/**
 * The factory to create the &lt;ao ...&gt; module descriptor.
 */
public final class ActiveObjectsModuleDescriptorFactory extends SingleModuleDescriptorFactory<ActiveObjectModuleDescriptor>
{
    private final OsgiServiceUtils osgiUtils;
    private final DataSourceTypeResolver dataSourceTypeResolver;
    private final Digester digester;
    private final TableNameConverter tableNameConverter;

    public ActiveObjectsModuleDescriptorFactory(HostContainer hostContainer, OsgiServiceUtils osgiUtils, DataSourceTypeResolver dataSourceTypeResolver, Digester digester, TableNameConverter tableNameConverter)
    {
        super(checkNotNull(hostContainer), "ao", ActiveObjectModuleDescriptor.class);
        this.osgiUtils = checkNotNull(osgiUtils);
        this.dataSourceTypeResolver = checkNotNull(dataSourceTypeResolver);
        this.digester = checkNotNull(digester);
        this.tableNameConverter = checkNotNull(tableNameConverter);
    }

    @Override
    public ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        return hasModuleDescriptor(type) ? new ActiveObjectModuleDescriptor(osgiUtils, dataSourceTypeResolver, digester) : null;
    }
}
