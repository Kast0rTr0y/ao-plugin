package com.atlassian.activeobjects.plugin;

import com.atlassian.activeobjects.EntitiesValidator;
import com.atlassian.activeobjects.admin.PluginToTablesMapping;
import com.atlassian.activeobjects.config.ActiveObjectsConfigurationFactory;
import com.atlassian.activeobjects.osgi.OsgiServiceUtils;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.hostcontainer.HostContainer;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;
import com.atlassian.tenancy.api.TenantAccessor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The factory to create the &lt;ao ...&gt; module descriptor.
 */
public final class ActiveObjectsModuleDescriptorFactory extends SingleModuleDescriptorFactory<ActiveObjectModuleDescriptor> {
    private final ModuleFactory moduleFactory;
    private final OsgiServiceUtils osgiUtils;
    private final PluginToTablesMapping pluginToTablesMapping;
    private final EntitiesValidator entitiesValidator;
    private final ActiveObjectsConfigurationFactory configurationFactory;
    private final TenantAccessor tenantAccessor;
    private final EventPublisher eventPublisher;

    public ActiveObjectsModuleDescriptorFactory(ModuleFactory moduleFactory,
                                                HostContainer hostContainer,
                                                ActiveObjectsConfigurationFactory configurationFactory,
                                                OsgiServiceUtils osgiUtils,
                                                PluginToTablesMapping pluginToTablesMapping,
                                                EntitiesValidator entitiesValidator,
                                                TenantAccessor tenantAccessor,
                                                EventPublisher eventPublisher) {
        super(checkNotNull(hostContainer), "ao", ActiveObjectModuleDescriptor.class);
        this.moduleFactory = checkNotNull(moduleFactory);
        this.configurationFactory = checkNotNull(configurationFactory);
        this.osgiUtils = checkNotNull(osgiUtils);
        this.pluginToTablesMapping = checkNotNull(pluginToTablesMapping);
        this.entitiesValidator = checkNotNull(entitiesValidator);
        this.tenantAccessor = checkNotNull(tenantAccessor);
        this.eventPublisher = checkNotNull(eventPublisher);
    }

    @Override
    public ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        return hasModuleDescriptor(type) ? new ActiveObjectModuleDescriptor(moduleFactory, configurationFactory, osgiUtils, pluginToTablesMapping, entitiesValidator, tenantAccessor, eventPublisher) : null;
    }
}
