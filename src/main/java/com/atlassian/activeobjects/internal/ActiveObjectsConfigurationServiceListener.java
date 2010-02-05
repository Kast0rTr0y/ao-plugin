package com.atlassian.activeobjects.internal;

import org.osgi.framework.ServiceReference;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

public class ActiveObjectsConfigurationServiceListener
{
    private final DatabaseDirectoryListener databaseDirectoryListener;

    public ActiveObjectsConfigurationServiceListener(DatabaseDirectoryListener databaseDirectoryListener)
    {
        this.databaseDirectoryListener = checkNotNull(databaseDirectoryListener);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void onActiveObjectsConfigurationServiceUpdated(ServiceReference reference)
    {
        databaseDirectoryListener.onDirectoryUpdated();
    }
}
