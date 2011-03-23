package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.Preconditions.*;

/**
 * A model version manager that uses SAL's {@link PluginSettings} to store the actual current version.
 * @see PluginSettingsFactory
 * @see PluginSettings
 */
public final class SalModelVersionManager implements ModelVersionManager
{
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final PluginSettingsFactory pluginSettingsFactory;
    private final ActiveObjectsSettingKeys settingKeys;

    public SalModelVersionManager(PluginSettingsFactory pluginSettingsFactory, ActiveObjectsSettingKeys settingKeys)
    {
        this.pluginSettingsFactory = checkNotNull(pluginSettingsFactory);
        this.settingKeys = checkNotNull(settingKeys);
    }

    @Override
    public ModelVersion getCurrent(Prefix tableNamePrefix)
    {
        final Lock read = lock.readLock();
        read.lock();
        try
        {
            return ModelVersion.valueOf((String) getPluginSettings().get(settingKeys.getModelVersionKey(tableNamePrefix)));
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
            getPluginSettings().put(settingKeys.getModelVersionKey(tableNamePrefix), version.toString());
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
}
