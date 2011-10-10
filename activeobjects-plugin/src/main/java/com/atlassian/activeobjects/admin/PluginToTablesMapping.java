package com.atlassian.activeobjects.admin;

import com.atlassian.plugin.Plugin;

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

        private PluginInfo(String key, String name, String version)
        {
            this.key = checkNotNull(key);
            this.name = checkNotNull(name);
            this.version = checkNotNull(version);
        }

        public static PluginInfo of(String key, String name, String version)
        {
            return new PluginInfo(key, name, version);
        }

        public static PluginInfo of(Plugin plugin)
        {
            return new PluginInfo(plugin.getKey(), plugin.getName(), plugin.getPluginInformation().getVersion());
        }
    }
}
