package com.atlassian.activeobjects.admin;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsPluginToTablesMapping implements PluginToTablesMapping
{
    private static final String KEY = ActiveObjectsPluginToTablesMapping.class.getName();

    private static final Type MAPPINGS_TYPE = new TypeToken<Map<String, PluginInfo>>()
    {
    }.getType();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final PluginSettings settings;
    private Map<String, PluginInfo> mappings;

    public ActiveObjectsPluginToTablesMapping(PluginSettingsFactory factory)
    {
        this.settings = checkNotNull(factory).createGlobalSettings();
    }

    @Override
    public void add(PluginInfo pluginInfo, List<String> tableNames)
    {
        lock.writeLock().lock();
        try
        {
            doAdd(pluginInfo, tableNames);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    @Override
    public PluginInfo get(String tableName)
    {
        lock.readLock().lock();
        try
        {
            return doGet(tableName);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    private void doAdd(PluginInfo pluginInfo, List<String> tableNames)
    {
        lazyInitMappings();
        final Map<String, PluginInfo> newMappings = Maps.newHashMap(mappings);
        for (String tableName : tableNames)
        {
            newMappings.put(tableName, pluginInfo);
        }
        putMapInSettings(newMappings);
        this.mappings = newMappings;
    }

    private PluginInfo doGet(String tableName)
    {
        lazyInitMappings();
        return mappings.get(tableName);
    }

    private void putMapInSettings(Map<String, PluginInfo> newMappings)
    {
        settings.put(KEY, new Gson().toJson(newMappings));
    }

    private void lazyInitMappings()
    {
        if (mappings == null)
        {
            final Map<String, PluginInfo> map = new Gson().fromJson((String) settings.get(KEY), MAPPINGS_TYPE);
            mappings = map != null ? map : Maps.<String, PluginInfo>newHashMap();
        }
    }
}
