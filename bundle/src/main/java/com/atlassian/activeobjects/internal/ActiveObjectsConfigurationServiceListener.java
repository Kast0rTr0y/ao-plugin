package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjectsConfiguration;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

public final class ActiveObjectsConfigurationServiceListener
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DatabaseDirectoryListener databaseDirectoryListener;

    public ActiveObjectsConfigurationServiceListener(DatabaseDirectoryListener databaseDirectoryListener)
    {
        this.databaseDirectoryListener = checkNotNull(databaseDirectoryListener);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    void onActiveObjectsConfigurationServiceUpdated(ServiceReference reference)
    {
        databaseDirectoryListener.onDirectoryUpdated();
    }

    public void onActiveObjectsConfigurationServiceBind(ServiceReference reference)
    {
        logger.debug("A new {} service has been bound, as {}", ActiveObjectsConfiguration.class, reference);
        onActiveObjectsConfigurationServiceUpdated(reference);
    }

    public void onActiveObjectsConfigurationServiceUnbind(ServiceReference reference)
    {
        logger.debug("Reference {} for service {} has been unbound", reference, ActiveObjectsConfiguration.class);
        onActiveObjectsConfigurationServiceUpdated(reference);
    }
}
