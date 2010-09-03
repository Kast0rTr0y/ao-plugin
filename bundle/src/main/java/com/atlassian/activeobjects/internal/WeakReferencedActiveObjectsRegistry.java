package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WeakReferencedActiveObjectsRegistry implements ActiveObjectsRegistry, DatabaseDirectoryListener
{
    private final Map<ActiveObjectsConfiguration, WeakReference<ActiveObjects>> cache = new HashMap<ActiveObjectsConfiguration, WeakReference<ActiveObjects>>();

    public synchronized ActiveObjects get(ActiveObjectsConfiguration configuration)
    {
        return cache.get(configuration) != null ? cache.get(configuration).get() : null;
    }

    public synchronized ActiveObjects register(ActiveObjectsConfiguration configuration, ActiveObjects ao)
    {
        cache.put(configuration, new WeakReference<ActiveObjects>(ao));
        return ao;
    }

    public synchronized void onDirectoryUpdated()
    {
        for (Iterator<Map.Entry<ActiveObjectsConfiguration, WeakReference<ActiveObjects>>> it = cache.entrySet().iterator(); it.hasNext();)
        {
            final Map.Entry<ActiveObjectsConfiguration, WeakReference<ActiveObjects>> aoEntry = it.next();
            if (aoEntry.getValue() != null && aoEntry.getValue().get() != null && aoEntry.getValue().get() instanceof DatabaseDirectoryAware)
            {
                it.remove();
            }
        }
    }
}
