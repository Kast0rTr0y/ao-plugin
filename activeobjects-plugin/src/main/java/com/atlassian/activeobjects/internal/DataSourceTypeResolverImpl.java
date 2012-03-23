package com.atlassian.activeobjects.internal;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;

/**
 * Sole implementation of {@link com.atlassian.activeobjects.internal.DataSourceTypeResolver},
 * configuration of data source type is 'simply' stored as a
 * {@link com.atlassian.sal.api.pluginsettings.PluginSettings plugin setting}.
 */
public final class DataSourceTypeResolverImpl implements DataSourceTypeResolver
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PluginSettings pluginSettings;
    private final DataSourceType defaultDataSourceType;
    private final ActiveObjectsSettingKeys settingKeys;

    public DataSourceTypeResolverImpl(PluginSettingsFactory pluginSettingsFactory, ActiveObjectsSettingKeys settingKeys, DataSourceType defaultDataSourceType)
    {
        checkNotNull(pluginSettingsFactory);
        this.pluginSettings = checkNotNull(pluginSettingsFactory.createGlobalSettings());
        this.settingKeys = checkNotNull(settingKeys);
        this.defaultDataSourceType = checkNotNull(defaultDataSourceType);
    }

    public DataSourceType getDataSourceType(Prefix prefix)
    {
        final String setting = getSetting(prefix);
        if (setting != null)
        {
            try
            {
                return DataSourceType.valueOf(setting);
            }
            catch (IllegalArgumentException e)
            {
                // if an incorrect value is stored, then we fall back on the default, not without a warning in the logs
                logger.warn("Active objects data source type setting <" + setting + "> for key <" + getSettingKey(prefix) + "> " +
                        "could not be resolved to a valid " + DataSourceType.class.getName() + ". Using default value" +
                        " <" + defaultDataSourceType + ">.");
                return defaultDataSourceType;
            }
        }
        else
        {
            return defaultDataSourceType;
        }
    }

    private String getSetting(Prefix prefix)
    {
        return (String) pluginSettings.get(getSettingKey(prefix));
    }

    private String getSettingKey(Prefix prefix)
    {
        return settingKeys.getDataSourceTypeKey(prefix);
    }
}
