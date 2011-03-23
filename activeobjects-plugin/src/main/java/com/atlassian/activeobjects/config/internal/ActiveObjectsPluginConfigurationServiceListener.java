package com.atlassian.activeobjects.config.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.DatabaseDirectoryAware;
import com.atlassian.activeobjects.spi.ActiveObjectsPluginConfiguration;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsPluginConfigurationServiceListener
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ActiveObjectsConfigurationListener activeObjectsConfigurationListener;

    public ActiveObjectsPluginConfigurationServiceListener(ActiveObjectsConfigurationListener activeObjectsConfigurationListener)
    {
        this.activeObjectsConfigurationListener = checkNotNull(activeObjectsConfigurationListener);
    }

    void onActiveObjectsConfigurationServiceUpdated(ServiceReference reference)
    {
        activeObjectsConfigurationListener.onConfigurationUpdated(new ConfigurationUpdatedPredicate()
        {
            public boolean matches(ActiveObjects activeObjects, ActiveObjectsConfiguration configuration)
            {
                return activeObjects != null && activeObjects instanceof DatabaseDirectoryAware;
            }
        });
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void onActiveObjectsConfigurationServiceBind(ServiceReference reference)
    {
        logger.debug("A new {} service has been bound, as {}", ActiveObjectsPluginConfiguration.class, reference);
        onActiveObjectsConfigurationServiceUpdated(reference);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void onActiveObjectsConfigurationServiceUnbind(ServiceReference reference)
    {
        logger.debug("Reference {} for service {} has been unbound", reference, ActiveObjectsPluginConfiguration.class);
        onActiveObjectsConfigurationServiceUpdated(reference);
    }
}
