package it.com.atlassian.activeobjects;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.factories.LegacyDynamicPluginFactory;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.loaders.DirectoryPluginLoader;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.manager.DefaultPluginManager;
import com.atlassian.plugin.manager.store.MemoryPluginPersistentStateStore;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.osgi.container.OsgiPersistentCache;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.container.felix.FelixOsgiContainerManager;
import com.atlassian.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import com.atlassian.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import com.atlassian.plugin.osgi.factory.OsgiBundleFactory;
import com.atlassian.plugin.osgi.factory.OsgiPluginFactory;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.repositories.FilePluginInstaller;
import org.junit.After;
import org.junit.Before;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.util.Arrays;

import static com.atlassian.activeobjects.test.IntegrationTestHelper.deleteDirectory;
import static com.atlassian.activeobjects.test.IntegrationTestHelper.getDir;
import static com.atlassian.activeobjects.test.IntegrationTestHelper.getTmpDir;

/**
 * Base for in-container unit tests
 */
public abstract class PluginInContainerTestBase
{
    private OsgiContainerManager osgiContainerManager;
    private File tmpDir;

    protected DefaultPluginManager pluginManager;

    @Before
    public final void setUp() throws Exception
    {
        tmpDir = getTmpDir("plugin-tmp");
    }

    @After
    public final void tearDown() throws Exception
    {
        stopOsgiContainer();
        deleteDirectory(tmpDir);

        osgiContainerManager = null;
        tmpDir = null;
        pluginManager = null;
    }

    protected final ServiceTracker getServiceTracker(Class<?> aClass)
    {
        final ServiceTracker tracker = new ServiceTracker(osgiContainerManager.getBundles()[0].getBundleContext(), aClass.getName(), null);
        tracker.open();
        return tracker;
    }

    protected final void initPluginManager(final HostComponentProvider hostComponentProvider) throws Exception
    {
        final PluginEventManager pluginEventManager = new DefaultPluginEventManager();
        final ModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        final PackageScannerConfiguration scannerConfig = buildScannerConfiguration(null);
        final HostComponentProvider requiredWrappingProvider = getHostComponentProvider(pluginEventManager, hostComponentProvider);
        final OsgiPersistentCache cache = new DefaultOsgiPersistentCache(getDir(tmpDir, "cache"));

        osgiContainerManager = new FelixOsgiContainerManager(cache, scannerConfig, requiredWrappingProvider, pluginEventManager);

        final LegacyDynamicPluginFactory legacyFactory = new LegacyDynamicPluginFactory(PluginAccessor.Descriptor.FILENAME, tmpDir);
        final OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(PluginAccessor.Descriptor.FILENAME, (String) null, cache, osgiContainerManager, pluginEventManager);
        final OsgiBundleFactory osgiBundleFactory = new OsgiBundleFactory(osgiContainerManager, pluginEventManager);

        final File pluginsDir = getDir(tmpDir, "plugins");

        final DirectoryPluginLoader loader = new DirectoryPluginLoader(pluginsDir, Arrays.asList(legacyFactory, osgiPluginDeployer, osgiBundleFactory), pluginEventManager);

        pluginManager = new DefaultPluginManager(new MemoryPluginPersistentStateStore(), Arrays.<PluginLoader>asList(loader), moduleDescriptorFactory, pluginEventManager);
        pluginManager.setPluginInstaller(new FilePluginInstaller(pluginsDir));
        pluginManager.init();
    }

    private void stopOsgiContainer()
    {
        if (osgiContainerManager != null)
        {
            osgiContainerManager.stop();
        }
    }

    private HostComponentProvider getHostComponentProvider(final PluginEventManager pluginEventManager, final HostComponentProvider hostComponentProvider)
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

    private PackageScannerConfiguration buildScannerConfiguration(String version)
    {
        final PackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration(version);
        scannerConfig.getPackageIncludes().add("com.atlassian.plugin*");
        scannerConfig.getPackageIncludes().add("javax.servlet*");
        scannerConfig.getPackageIncludes().add("com_cenqua_clover");
        scannerConfig.getPackageIncludes().add("it.com.atlassian.activeobjects");
        scannerConfig.getPackageIncludes().add("org.dom4j*");
        scannerConfig.getPackageExcludes().add("com.atlassian.plugin.osgi.bridge*");
        scannerConfig.getPackageExcludes().add("com.atlassian.plugin.web.springmvc*");
        scannerConfig.getPackageExcludes().add("com.atlassian.activeobjects*");
        return scannerConfig;
    }
}
