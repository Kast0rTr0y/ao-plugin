package com.atlassian.labs.activeobjects.internal;

import com.atlassian.labs.activeobjects.internal.ActiveObjectsProvider;
import com.atlassian.labs.activeobjects.external.ActiveObjects;
import com.atlassian.labs.activeobjects.external.ActiveObjectsConfiguration;
import com.atlassian.sal.api.ApplicationProperties;

import java.io.File;
import java.util.HashMap;
import java.lang.ref.WeakReference;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 */
public class DefaultActiveObjectsProvider implements ActiveObjectsProvider
{
    private final File databaseBaseDirectory;
    private final HashMap<String, WeakReference<ActiveObjects>> activeObjectCache;
    private final Logger log = LoggerFactory.getLogger(DefaultActiveObjectsProvider.class);

    public DefaultActiveObjectsProvider(ApplicationProperties appProps, ActiveObjectsConfiguration config)
    {
        File home = appProps.getHomeDirectory();
        if (home == null || !home.exists())
        {
            throw new RuntimeException("This plugin requires a home directory");
        }
        String path = config.getDatabaseBaseDirectory();
        if (path.startsWith("/"))
        {
            path = path.substring(1);
        }
        databaseBaseDirectory = new File(home, path);
        if (!databaseBaseDirectory.exists())
        {
            databaseBaseDirectory.mkdirs();
        }
        log.debug("ActiveObjects database directory {} initialized", databaseBaseDirectory.getAbsolutePath());

        activeObjectCache = new HashMap<String, WeakReference<ActiveObjects>>();
    }

    /**
     * Gets an {@link ActiveObjects} instance per identifier.  Instances are pooled so multiple calls of the same
     * identifier will return identical results.
     * @param pluginKey The db or plugin identifier
     * @return The instance
     */
    public synchronized ActiveObjects createActiveObjects(String pluginKey)
    {
        ActiveObjects ao = null;
        WeakReference<ActiveObjects> ref = activeObjectCache.get(pluginKey);
        if (ref != null)
        {
            ao = ref.get();
            if (ao == null)
            {
                activeObjectCache.remove(pluginKey);
            }
        }

        if (ao == null)
        {
            File dbdir = new File(databaseBaseDirectory, pluginKey);
            if (!dbdir.exists())
            {
                dbdir.mkdir();
            }
            ao = new DefaultActiveObjects("jdbc:hsqldb:file:" + dbdir.getAbsolutePath() + "/db", "sa", "");
            activeObjectCache.put(pluginKey, new WeakReference<ActiveObjects>(ao));
            log.debug("Created ActiveObjects instance {} at {}", pluginKey, dbdir.getAbsolutePath());
        }
        return ao;
    }
}
