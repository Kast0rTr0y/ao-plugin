package com.atlassian.activeobjects.admin;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginInformation;
import com.google.common.base.Preconditions;

import java.util.List;

import static com.google.common.base.Preconditions.*;

public interface PluginToTablesMapping
{
    void add(ActiveObjectsPluginToTablesMapping.PluginInfo pluginInfo, List<String> tableNames);

    PluginInfo get(String tableName);

    public final static class PluginInfo
    {
        public final String key;
        public final String name;
        public final String version;
        public final String vendorName;
        public final String vendorUrl;

        private PluginInfo(String key, String name, String version, String vendorName, String vendorUrl)
        {
            this.key = checkNotNull(key);
            this.name = checkNotNull(name);
            this.version = checkNotNull(version);
            this.vendorName = vendorName;
            this.vendorUrl = vendorUrl;
        }

        public static PluginInfo of(Plugin plugin)
        {
            final PluginInformation pluginInformation = plugin.getPluginInformation();
            return new PluginInfo(plugin.getKey(), plugin.getName(), pluginInformation.getVersion(), pluginInformation.getVendorName(), pluginInformation.getVendorUrl());
        }
    }
}
