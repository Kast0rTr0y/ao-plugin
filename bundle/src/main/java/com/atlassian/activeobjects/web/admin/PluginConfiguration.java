package com.atlassian.activeobjects.web.admin;

import com.atlassian.plugin.Plugin;

import java.util.Map;

public class PluginConfiguration
{
    private Plugin plugin;
    private Map<String, String> entities;

    public Plugin getPlugin()
    {
        return plugin;
    }

    public void setPlugin(Plugin plugin)
    {
        this.plugin = plugin;
    }

    public Map<String, String> getEntities()
    {
        return entities;
    }

    public void setEntities(Map<String, String> entities)
    {
        this.entities = entities;
    }
}
