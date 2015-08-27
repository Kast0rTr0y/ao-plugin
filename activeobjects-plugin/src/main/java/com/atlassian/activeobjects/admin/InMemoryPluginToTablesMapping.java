package com.atlassian.activeobjects.admin;

import com.google.common.annotations.VisibleForTesting;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryPluginToTablesMapping implements PluginToTablesMapping {
    @VisibleForTesting
    final ConcurrentMap<String, PluginInfo> pluginInfoByTableName = new ConcurrentHashMap<String, PluginInfo>();

    @Override
    public void add(final PluginInfo pluginInfo, final List<String> tableNames) {
        for (String tableName : tableNames) {
            pluginInfoByTableName.put(tableName, pluginInfo);
        }
    }

    @Override
    public PluginInfo get(final String tableName) {
        return pluginInfoByTableName.get(tableName);
    }
}
