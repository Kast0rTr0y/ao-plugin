package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.config.internal.ActiveObjectsConfigurationListener;
import com.atlassian.activeobjects.config.internal.ConfigurationUpdatedPredicate;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class WeakReferencedActiveObjectsRegistry implements ActiveObjectsRegistry, ActiveObjectsConfigurationListener
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

    public synchronized void onConfigurationUpdated(ConfigurationUpdatedPredicate predicate)
    {
        for (Iterator<Map.Entry<ActiveObjectsConfiguration, WeakReference<ActiveObjects>>> it = cache.entrySet().iterator(); it.hasNext();)
        {
            final Map.Entry<ActiveObjectsConfiguration, WeakReference<ActiveObjects>> aoEntry = it.next();
            if (predicate.matches(aoEntry.getValue().get(), aoEntry.getKey()))
            {
                it.remove();
            }
        }
    }
}
