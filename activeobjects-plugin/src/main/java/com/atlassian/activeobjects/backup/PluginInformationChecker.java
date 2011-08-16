package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.importer.DatabaseInformationChecker;

import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

final class PluginInformationChecker implements DatabaseInformationChecker
{
    private DatabaseInformation information = new DatabaseInformation(Collections.<String, String>emptyMap());

    @Override
    public void check(DatabaseInformation information)
    {
        this.information = checkNotNull(information);
    }

    public PluginInformation getPluginInformation()
    {
        return new PluginInformation()
        {
            @Override
            public boolean isAvailable()
            {
                return information.get(PluginInformationReader.PLUGIN_KEY, new ExistsStringConverter(), false);
            }

            @Override
            public String getPluginName()
            {
                return information.getString(PluginInformationReader.PLUGIN_NAME, null);
            }

            @Override
            public String getPluginKey()
            {
                return information.getString(PluginInformationReader.PLUGIN_KEY, null);
            }

            @Override
            public String getPluginVersion()
            {
                return information.getString(PluginInformationReader.PLUGIN_VERSION, null);
            }

            @Override
            public String getHash()
            {
                return information.get(PluginInformationReader.PLUGIN_AO_HASH, null);
            }
        };
    }

    private static class ExistsStringConverter extends DatabaseInformation.AbstractStringConverter<Boolean>
    {
        @Override
        public Boolean convert(String s)
        {
            return s != null;
        }
    }
}
