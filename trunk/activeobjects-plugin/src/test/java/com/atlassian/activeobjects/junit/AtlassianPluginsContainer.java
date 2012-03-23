package com.atlassian.activeobjects.junit;

import com.atlassian.plugin.Plugin;

import java.io.File;

public interface AtlassianPluginsContainer
{
    void start();

    void stop();

    void restart();

    /**
     * Installs the file as a plugin
     *
     * @param file the plugin file
     * @return the plugin that was just installed
     * @see #unInstall(com.atlassian.plugin.Plugin)
     */
    Plugin install(File file);

    void unInstall(Plugin plugin);

    <T> T getService(Class<T> serviceType) throws InterruptedException;
}
