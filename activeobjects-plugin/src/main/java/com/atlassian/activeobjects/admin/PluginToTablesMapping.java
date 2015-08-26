package com.atlassian.activeobjects.admin;

import java.util.List;

public interface PluginToTablesMapping {
    void add(PluginInfo pluginInfo, List<String> tableNames);

    PluginInfo get(String tableName);
}
