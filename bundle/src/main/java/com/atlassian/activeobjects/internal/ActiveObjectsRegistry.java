package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;

public interface ActiveObjectsRegistry
{
    ActiveObjects get(PluginKey pluginKey);

    ActiveObjects register(PluginKey pluginKey, ActiveObjects ao);
}
