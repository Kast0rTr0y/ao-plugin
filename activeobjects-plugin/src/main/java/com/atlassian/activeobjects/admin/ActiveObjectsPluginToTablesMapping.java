package com.atlassian.activeobjects.admin;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ActiveObjectsPluginToTablesMapping implements PluginToTablesMapping
{
    private static final String KEY = ActiveObjectsPluginToTablesMapping.class.getName();

    private static final Type MAPPINGS_TYPE = new TypeToken<Map<String, PluginInfo>>()
    {
    }.getType();

    private final PluginSettings settings;

    public ActiveObjectsPluginToTablesMapping(PluginSettingsFactory factory)
    {
        this.settings = checkNotNull(factory).createGlobalSettings();
    }

    @Override
    public synchronized void add(PluginInfo pluginInfo, List<String> tableNames)
    {
        final Map<String, PluginInfo> mappings = getMappingFromSettings();
        for (String tableName : tableNames)
        {
            mappings.put(tableName, pluginInfo);
        }
        settings.put(KEY, mappings);
    }

    @Override
    public PluginInfo get(String tableName)
    {
        return getMappingFromSettings().get(tableName);
    }

    protected Map<String, PluginInfo> getMappingFromSettings()
    {
        final Map<String, PluginInfo> map = new Gson().fromJson((String) settings.get(KEY), MAPPINGS_TYPE);
        return map != null ? map : Maps.<String, PluginInfo>newHashMap();
    }
}
