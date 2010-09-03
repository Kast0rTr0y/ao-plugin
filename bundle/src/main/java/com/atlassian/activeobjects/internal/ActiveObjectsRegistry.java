package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;

public interface ActiveObjectsRegistry
{
    ActiveObjects get(ActiveObjectsConfiguration configuration);

    ActiveObjects register(ActiveObjectsConfiguration configuration, ActiveObjects ao);
}
