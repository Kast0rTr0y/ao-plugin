package com.atlassian.activeobjects.junit;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.factories.LegacyDynamicPluginFactory;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.loaders.DirectoryPluginLoader;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.manager.DefaultPluginManager;
import com.atlassian.plugin.manager.store.MemoryPluginPersistentStateStore;
import com.atlassian.plugin.osgi.container.OsgiPersistentCache;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import com.atlassian.plugin.osgi.factory.OsgiBundleFactory;
import com.atlassian.plugin.osgi.factory.OsgiPluginFactory;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.repositories.FilePluginInstaller;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;

public final class JUnitAtlassianPluginsContainer implements AtlassianPluginsContainer
{
    private DefaultPluginManager pluginManager;
    private FelixOsgiContainerManager osgiContainerManager;

    public JUnitAtlassianPluginsContainer(AtlassianPluginsContainerConfiguration configuration)
    {
        initPluginManager(configuration.getHostComponentProvider(), configuration.getPackageScannerConfiguration(), configuration.getTmpDir());
    }

    @Override
    public void start()
    {
        pluginManager.init();
    }

    @Override
    public void stop()
    {
        pluginManager.shutdown();
    }

    @Override
    public void restart()
    {
        pluginManager.warmRestart();
    }

    @Override
    public Plugin install(File file)
    {
        return getPlugin(pluginManager.installPlugin(newPluginArtifact(file)));
    }

    @Override
    public void unInstall(Plugin plugin)
    {
        pluginManager.uninstall(plugin);
    }

    private Plugin getPlugin(String key)
    {
        return pluginManager.getPlugin(key);
    }

    @Override
    public <T> T getService(Class<T> serviceType) throws InterruptedException
    {
        final ServiceTracker tracker = new ServiceTracker(osgiContainerManager.getBundles()[0].getBundleContext(), serviceType.getName(), null);
        tracker.open();
        return serviceType.cast(tracker.waitForService(10000));
    }

    private void initPluginManager(HostComponentProvider hostComponentProvider, PackageScannerConfiguration scannerConfiguration, File tmpDir)
    {
        final PluginEventManager pluginEventManager = new DefaultPluginEventManager();
        final ModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        final HostComponentProvider requiredWrappingProvider = newWrappingHostComponentProvider(hostComponentProvider, pluginEventManager);
        final OsgiPersistentCache cache = new DefaultOsgiPersistentCache(getDir(tmpDir, "cache"));
        osgiContainerManager = new FelixOsgiContainerManager(cache, scannerConfiguration, requiredWrappingProvider, pluginEventManager);

        final LegacyDynamicPluginFactory legacyFactory = new LegacyDynamicPluginFactory(PluginAccessor.Descriptor.FILENAME, tmpDir);
        final OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(PluginAccessor.Descriptor.FILENAME, (String) null, cache, osgiContainerManager, pluginEventManager);
        final OsgiBundleFactory osgiBundleFactory = new OsgiBundleFactory(osgiContainerManager, pluginEventManager);

        final File pluginsDir = getDir(tmpDir, "plugins");
        final DirectoryPluginLoader loader = new DirectoryPluginLoader(pluginsDir, Arrays.asList(legacyFactory, osgiPluginDeployer, osgiBundleFactory), pluginEventManager);

        pluginManager = new DefaultPluginManager(new MemoryPluginPersistentStateStore(), Arrays.<PluginLoader>asList(loader), moduleDescriptorFactory, pluginEventManager);
        pluginManager.setPluginInstaller(new FilePluginInstaller(pluginsDir));
    }

    private static HostComponentProvider newWrappingHostComponentProvider(final HostComponentProvider hostComponentProvider, final PluginEventManager pluginEventManager)
    {
        return new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(PluginEventManager.class).forInstance(pluginEventManager);
                if (hostComponentProvider != null)
                {
                    hostComponentProvider.provide(registrar);
                }
            }
        };
    }

    private static JarPluginArtifact newPluginArtifact(File file)
    {
        return new JarPluginArtifact(file);
    }

    private static File getDir(File parentDir, String name)
    {
        return mkdirs(new File(parentDir, name));
    }

    private static File mkdirs(final File dir)
    {
        assertTrue(dir.mkdirs());
        return dir;
    }
}
