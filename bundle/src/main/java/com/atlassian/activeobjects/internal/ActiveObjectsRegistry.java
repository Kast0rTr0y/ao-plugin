package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;

public interface ActiveObjectsRegistry
{
    ActiveObjects get(String pluginKey);

    ActiveObjects register(String pluginKey, ActiveObjects ao);
}
