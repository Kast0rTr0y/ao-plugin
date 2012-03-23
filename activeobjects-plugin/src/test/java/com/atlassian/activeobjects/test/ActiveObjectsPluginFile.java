package com.atlassian.activeobjects.test;

import com.atlassian.activeobjects.junit.PluginFile;

import java.io.File;

import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsPluginFile implements PluginFile
{
    private static final String PLUGIN_JAR_PATH_PROPERTY_NAME = "plugin.jar";

    @Override
    public File getPluginFile()
    {
        return new File(getPluginPath());
    }

    private String getPluginPath()
    {
        final String path = System.getProperty(PLUGIN_JAR_PATH_PROPERTY_NAME);

        final StringBuilder errorMsg = new StringBuilder(180)
                .append("\n")
                .append("Could not find plugin jar path. System property '")
                .append(PLUGIN_JAR_PATH_PROPERTY_NAME).append("' is not set.\n")
                .append("If you're NOT running your tests using maven, make sure to set the system property in your (IDE) test configuration.");

        return checkNotNull(path, errorMsg);
    }
}
