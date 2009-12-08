package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjectsConfiguration;

import java.io.File;

/**
 *
 */
public class DefaultActiveObjectsConfiguration implements ActiveObjectsConfiguration
{
    public String getDatabaseBaseDirectory() {
        return "data" + File.separatorChar + "plugins" + File.separatorChar + "activeobjects";
    }
}
