package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.dbexporter.exporter.DatabaseInformationReader;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class PluginInformationReader implements DatabaseInformationReader
{
    static final String PLUGIN_NAME = "plugin.name";
    static final String PLUGIN_KEY = "plugin.key";
    static final String PLUGIN_VERSION = "plugin.version";
    static final String PLUGIN_AO_HASH = "plugin.ao.hash";

    private final PluginInformation pluginInfo;

    public PluginInformationReader(PluginInformation pluginInfo)
    {
        this.pluginInfo = checkNotNull(pluginInfo);
    }

    @Override
    public Map<String, String> get()
    {
        return pluginInfo.isAvailable() ? asMap(pluginInfo) : Collections.<String, String>emptyMap();
    }

    private static Map<String, String> asMap(PluginInformation pluginInfo)
    {
        return ImmutableMap.of(
                PLUGIN_NAME, pluginInfo.getPluginName(),
                PLUGIN_KEY, pluginInfo.getPluginKey(),
                PLUGIN_VERSION, pluginInfo.getPluginVersion(),
                PLUGIN_AO_HASH, pluginInfo.getHash()
        );
    }
}
