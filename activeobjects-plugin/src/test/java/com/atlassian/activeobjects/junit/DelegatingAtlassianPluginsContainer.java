package com.atlassian.activeobjects.junit;

import com.atlassian.plugin.Plugin;

import java.io.File;

import static com.google.common.base.Preconditions.*;

/**
 * A simple delegating container, it is convenient when one wants to change the behavior of one method only without the
 * need to re-implement the whole interface.
 *
 * @see AtlassianPluginsMethodRule#before AtlassianPluginsMethodRule#before implementation for usage example
 */
abstract class DelegatingAtlassianPluginsContainer implements AtlassianPluginsContainer
{
    private final AtlassianPluginsContainer container;

    public DelegatingAtlassianPluginsContainer(AtlassianPluginsContainer container)
    {
        this.container = checkNotNull(container);
    }

    @Override
    public void start()
    {
        container.start();
    }

    @Override
    public void stop()
    {
        container.stop();
    }

    @Override
    public void restart()
    {
        container.restart();
    }

    @Override
    public Plugin install(File file)
    {
        return container.install(file);
    }

    @Override
    public void unInstall(Plugin plugin)
    {
        container.unInstall(plugin);
    }

    @Override
    public <T> T getService(Class<T> serviceType) throws InterruptedException
    {
        return container.getService(serviceType);
    }
}
