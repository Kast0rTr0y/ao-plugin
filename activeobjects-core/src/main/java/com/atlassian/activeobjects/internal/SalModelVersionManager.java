package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.*;

public final class SalModelVersionManager implements ModelVersionManager
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String AO_MODEL_VERSION = "#";

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final PluginSettingsFactory pluginSettingsFactory;

    public SalModelVersionManager(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = checkNotNull(pluginSettingsFactory);
    }

    @Override
    public ModelVersion getCurrent(Prefix tableNamePrefix)
    {
        final Lock read = lock.readLock();
        read.lock();
        try
        {
            return ModelVersion.valueOf((String) getPluginSettings().get(getKey(tableNamePrefix)));
        }
        finally
        {
            read.unlock();
        }
    }

    @Override
    public void update(Prefix tableNamePrefix, ModelVersion version)
    {
        final Lock write = lock.writeLock();
        write.lock();
        try
        {
            getPluginSettings().put(getKey(tableNamePrefix), version.toString());
        }
        finally
        {
            write.unlock();
        }
    }

    private PluginSettings getPluginSettings()
    {
        return pluginSettingsFactory.createGlobalSettings();
    }

    private String getKey(Prefix tableNamePrefix)
    {
        final String key = tableNamePrefix.prepend(AO_MODEL_VERSION);
        logger.debug("Plugin settings key is {}", key);
        return key;
    }
}
