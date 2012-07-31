package com.atlassian.activeobjects.config.internal;

import com.atlassian.activeobjects.spi.ActiveObjectsPluginConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.service.ServiceUnavailableException;

import static com.google.common.base.Preconditions.*;

/**
 * This is the default active objects plugin configuration, which will delegate to another configuration and nicely
 * fallback onto default configurations if the delegate is not available (in the case of an OSGi service, configured
 * through spring)
 */
public final class DefaultActiveObjectsPluginConfiguration implements ActiveObjectsPluginConfiguration
{
    private static final String DEFAULT_BASE_DIR = "data/plugins/activeobjects";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ActiveObjectsPluginConfiguration delegate;

    public DefaultActiveObjectsPluginConfiguration(ActiveObjectsPluginConfiguration delegate)
    {
        this.delegate = checkNotNull(delegate);
    }

    public String getDatabaseBaseDirectory()
    {
        try
        {
            return delegate.getDatabaseBaseDirectory();
        }
        catch (ServiceUnavailableException e)
        {
            logger.debug("Active objects plugin configuration service not present, so using default base directory <{}>", DEFAULT_BASE_DIR);
            return DEFAULT_BASE_DIR;
        }
    }
}
