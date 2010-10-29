package com.atlassian.activeobjects.internal;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

/**
 * Sole implementation of {@link com.atlassian.activeobjects.internal.DataSourceTypeResolver},
 * configuration of data source type is 'simply' stored as a
 * {@link com.atlassian.sal.api.pluginsettings.PluginSettings plugin setting}.
 * @see #getSettingKey(PluginKey)
 */
public final class DataSourceTypeResolverImpl implements DataSourceTypeResolver
{
    private static final String PLUGIN_SETTING_KEY_PREFIX = "com.atlassian.activeobjects";
    private static final String DATA_SOURCE_TYPE_KEY = "dataSourceType";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PluginSettings pluginSettings;
    private final DataSourceType defaultDataSourceType;

    public DataSourceTypeResolverImpl(PluginSettingsFactory pluginSettingsFactory, DataSourceType defaultDataSourceType)
    {
        this.pluginSettings = checkNotNull(pluginSettingsFactory).createGlobalSettings(); // TODO: is this a good idea?
        this.defaultDataSourceType = checkNotNull(defaultDataSourceType);
    }

    public DataSourceType getDataSourceType(PluginKey pluginKey)
    {
        final String setting = getSetting(pluginKey);
        if (setting != null)
        {
            try
            {
                return DataSourceType.valueOf(setting);
            }
            catch (IllegalArgumentException e)
            {
                // if an incorrect value is stored, then we fall back on the default, not without a warning in the logs
                logger.warn("Active objects data source type setting <" + setting + "> for plugin <" + pluginKey + "> " +
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

    private String getSetting(PluginKey pluginKey)
    {
        return (String) pluginSettings.get(getSettingKey(pluginKey));
    }

    private String getSettingKey(PluginKey pluginKey)
    {
        return PLUGIN_SETTING_KEY_PREFIX + ":" + pluginKey.toString() + ":" + DATA_SOURCE_TYPE_KEY;
    }
}
