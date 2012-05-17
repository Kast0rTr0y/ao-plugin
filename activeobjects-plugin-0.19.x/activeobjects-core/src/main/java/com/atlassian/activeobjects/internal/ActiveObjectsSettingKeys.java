package com.atlassian.activeobjects.internal;

/**
 * This represents the setting keys that Active Objects is using/persisting in a host application
 *
 * @see com.atlassian.sal.api.pluginsettings.PluginSettingsFactory
 */
public final class ActiveObjectsSettingKeys
{
    public static final String MODEL_VERSION = "#";
    public static final String DATA_SOURCE_TYPE = "DST";

    public String getDataSourceTypeKey(Prefix prefix)
    {
        return prefix.prepend(DATA_SOURCE_TYPE);
    }

    public String getModelVersionKey(Prefix prefix)
    {
        return prefix.prepend(MODEL_VERSION);
    }
}
