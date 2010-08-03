package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjectsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.service.ServiceUnavailableException;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

public class DefaultDatabaseConfiguration implements DatabaseConfiguration
{
    private static final String DEFAULT_BASE_DIR = "data/plugins/activeobjects";

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ActiveObjectsConfiguration configuration;

    public DefaultDatabaseConfiguration(ActiveObjectsConfiguration configuration)
    {
        this.configuration = checkNotNull(configuration);
    }

    public String getBaseDirectory()
    {
        try
        {
            return configuration.getDatabaseBaseDirectory();
        }
        catch (ServiceUnavailableException e)
        {
            log.debug("Active objects configuration service not present, so using default base directory <{}>", DEFAULT_BASE_DIR);
            return DEFAULT_BASE_DIR;
        }
    }
}
