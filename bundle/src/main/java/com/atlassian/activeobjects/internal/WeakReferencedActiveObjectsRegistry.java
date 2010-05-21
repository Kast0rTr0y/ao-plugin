package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WeakReferencedActiveObjectsRegistry implements ActiveObjectsRegistry, DatabaseDirectoryListener
{
    private final Map<String, WeakReference<ActiveObjects>> cache = new HashMap<String, WeakReference<ActiveObjects>>();

    public synchronized ActiveObjects get(String pluginKey)
    {
        return cache.get(pluginKey) != null ? cache.get(pluginKey).get() : null;
    }

    public synchronized ActiveObjects register(String pluginKey, ActiveObjects ao)
    {
        cache.put(pluginKey, new WeakReference<ActiveObjects>(ao));
        return ao;
    }

    public synchronized void onDirectoryUpdated()
    {
        for (Iterator<Map.Entry<String, WeakReference<ActiveObjects>>> it = cache.entrySet().iterator(); it.hasNext();)
        {
            final Map.Entry<String, WeakReference<ActiveObjects>> aoEntry = it.next();
            if (aoEntry.getValue() != null && aoEntry.getValue().get() != null && aoEntry.getValue().get() instanceof DatabaseDirectoryAware)
            {
                it.remove();
            }
        }
    }
}
