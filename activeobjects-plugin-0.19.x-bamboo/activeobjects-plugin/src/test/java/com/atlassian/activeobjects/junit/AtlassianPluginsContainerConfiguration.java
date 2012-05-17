package com.atlassian.activeobjects.junit;

import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;

import java.io.File;

public interface AtlassianPluginsContainerConfiguration
{
    HostComponentProvider getHostComponentProvider();

    PackageScannerConfiguration getPackageScannerConfiguration();

    File getTmpDir();
}
