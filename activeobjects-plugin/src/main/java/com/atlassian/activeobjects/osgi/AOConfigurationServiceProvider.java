package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import org.osgi.framework.Bundle;

public interface AOConfigurationServiceProvider
{
    ActiveObjectsConfiguration generateScannedConfiguration(Bundle bundle);

    String getEntityDefaultPackage();
}
