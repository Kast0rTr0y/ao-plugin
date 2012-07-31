package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.spi.ActiveObjectsPluginConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.service.ServiceUnavailableException;

import static com.google.common.base.Preconditions.*;

public class DefaultDatabaseConfiguration implements DatabaseConfiguration
{
    private static final String DEFAULT_BASE_DIR = "data/plugins/activeobjects";

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ActiveObjectsPluginConfiguration pluginConfiguration;

    public DefaultDatabaseConfiguration(ActiveObjectsPluginConfiguration pluginConfiguration)
    {
        this.pluginConfiguration = checkNotNull(pluginConfiguration);
    }

    public String getBaseDirectory()
    {
        try
        {
            return pluginConfiguration.getDatabaseBaseDirectory();
        }
        catch (ServiceUnavailableException e)
        {
            log.debug("Active objects plugin configuration service not present, so using default base directory <{}>", DEFAULT_BASE_DIR);
            return DEFAULT_BASE_DIR;
        }
    }
}
