package it.com.atlassian.activeobjects;

import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.test.PluginTestUtils;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.backup.BackupRegistry;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TestIntegrations extends PluginInContainerTestBase
{
    private File baseDir;
    private ServiceTracker runTracker;
    private ApplicationProperties props;
    private HostComponentProvider defHostComponentProvider;

    public void setUp() throws Exception
    {
        super.setUp();
        props = mock(ApplicationProperties.class);
        baseDir = PluginTestUtils.createTempDirectory(getClass());
        when(props.getHomeDirectory()).thenReturn(baseDir);
        defHostComponentProvider = new HostComponentProvider()
        {
            public void provide(ComponentRegistrar componentRegistrar)
            {
                componentRegistrar.register(ApplicationProperties.class).forInstance(props);
                componentRegistrar.register(DataSourceProvider.class).forInstance(mock(DataSourceProvider.class));
                componentRegistrar.register(BackupRegistry.class).forInstance(mock(BackupRegistry.class));
            }
        };
    }

    private void initPluginManagerWithActiveObjects(HostComponentProvider prov) throws Exception
    {
        initPluginManager(prov);
        String activeObjectsPluginKey = pluginManager.installPlugin(new JarPluginArtifact(new File(System.getProperty("plugin.jar"))));
        assertTrue(pluginManager.isPluginEnabled(activeObjectsPluginKey));

        runTracker = new ServiceTracker(osgiContainerManager.getBundles()[0].getBundleContext(), ActiveObjectsTestConsumer.class.getName(), null);
        runTracker.open();
    }

    public void testBasic() throws Exception
    {
        initPluginManagerWithActiveObjects(defHostComponentProvider);
        File plugin = buildConsumerPlugin("test-consumer");

        pluginManager.installPlugin(new JarPluginArtifact(plugin));
        assertTrue(pluginManager.isPluginEnabled("test-consumer"));
        callActiveObjectsConsumer();
        assertDatabaseExists(baseDir, "data/plugins/activeobjects", "test-");
    }

    public void testBasicWithConfig() throws Exception
    {
        initPluginManagerWithActiveObjects(defHostComponentProvider);
        File plugin = buildConsumerPlugin("test-consumer");
        File configPlugin = buildConfigPlugin("foo");

        final String configPluginKey = pluginManager.installPlugin(new JarPluginArtifact(configPlugin));
        pluginManager.installPlugin(new JarPluginArtifact(plugin));

        callActiveObjectsConsumer();
        assertTrue(pluginManager.isPluginEnabled("test-consumer"));
        assertDatabaseExists(baseDir, "foo", "test-");

        pluginManager.uninstall(pluginManager.getPlugin(configPluginKey));

        configPlugin = buildConfigPlugin("foo2");
        pluginManager.installPlugin(new JarPluginArtifact(configPlugin));

        callActiveObjectsConsumer();
        assertDatabaseExists(baseDir, "foo2", "test-");
    }

    public void testClientSurvivesRequiredDepChange() throws Exception
    {
        initPluginManagerWithActiveObjects(defHostComponentProvider);
        File childBaseDir = new File(baseDir, "child");
        childBaseDir.mkdir();
        File plugin = buildConsumerPlugin("test-consumer");

        pluginManager.installPlugin(new JarPluginArtifact(plugin));
        assertDatabaseDoesNotExists(childBaseDir, "data/plugins/activeobjects", "test-");

        when(props.getHomeDirectory()).thenReturn(childBaseDir);
        pluginManager.warmRestart();
        callActiveObjectsConsumer();
        assertDatabaseExists(childBaseDir, "data/plugins/activeobjects", "test-");
    }

    public void testClientCallsWhenDown() throws Exception
    {
        final AtomicBoolean shouldExpose = new AtomicBoolean(true);
        final HostComponentProvider componentProvider = new HostComponentProvider()
        {
            public void provide(ComponentRegistrar componentRegistrar)
            {
                if (shouldExpose.get())
                {
                    componentRegistrar.register(ApplicationProperties.class).forInstance(props);
                }
                componentRegistrar.register(DataSourceProvider.class).forInstance(mock(DataSourceProvider.class));
                componentRegistrar.register(BackupRegistry.class).forInstance(mock(BackupRegistry.class));
            }
        };
        initPluginManagerWithActiveObjects(componentProvider);
        File plugin = buildConsumerPlugin("test-consumer");
        pluginManager.installPlugin(new JarPluginArtifact(plugin));
        shouldExpose.set(false);
        pluginManager.warmRestart();

        long start = System.currentTimeMillis();
        try
        {
            callActiveObjectsConsumer();
            fail("Should have thrown an exception");
        }
        catch (RuntimeException e)
        {
            assertEquals("service matching filter=[(objectClass=com.atlassian.sal.api.ApplicationProperties)] unavailable", e.getMessage());
            assertTrue(System.currentTimeMillis() > start + 5000);
        }
    }


    // Test disabled until ActiveObjects is upgraded past 0.8.2

    public void _testBasicWithLotsOfConcurrentCalls() throws Exception
    {
        initPluginManagerWithActiveObjects(defHostComponentProvider);
        File plugin = buildConsumerPlugin("test-consumer");

        pluginManager.installPlugin(new JarPluginArtifact(plugin));
        assertTrue(pluginManager.isPluginEnabled("test-consumer"));

        final ActiveObjectsTestConsumer runnable = (ActiveObjectsTestConsumer) runTracker.waitForService(10000);
        runnable.init();

        final AtomicBoolean failFlag = new AtomicBoolean(false);
        Runnable r = new Runnable()
        {
            int count = 0;

            public void run()
            {
                if (!failFlag.get())
                {
                    System.out.println("calling " + (count++));
                    try
                    {
                        runnable.run();
                    }
                    catch (Exception e)
                    {
                        failFlag.set(true);
                        e.printStackTrace();
                    }
                }
            }
        };
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int x = 0; x < 1000; x++)
        {
            executor.execute(r);
        }
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        assertFalse(failFlag.get());
        assertDatabaseExists(baseDir, "data/plugins/activeobjects", "test-");
    }


    private void callActiveObjectsConsumer() throws Exception
    {
        ActiveObjectsTestConsumer runnable = (ActiveObjectsTestConsumer) runTracker.waitForService(10000);
        runnable.init();
        runnable.run();
    }

    private void assertDatabaseExists(File baseDir, String path, final String dbprefix)
    {
        assertDatabaseExists(baseDir, path, dbprefix, 1);
    }

    private void assertDatabaseDoesNotExists(File baseDir, String path, final String dbprefix)
    {
        assertDatabaseExists(baseDir, path, dbprefix, 0);
    }

    private void assertDatabaseExists(File baseDir, String path, final String dbprefix, int expected)
    {
        final File[] files = new File(baseDir, path).listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.startsWith(dbprefix);
            }
        });
        if (expected == 0)
        {
            assertNull(files);
        }
        else
        {
            assertEquals(expected, files.length);
        }
    }

    private File buildConsumerPlugin(String key) throws Exception
    {
        return new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public interface Foo extends net.java.ao.Entity {",
                        " public String getName();",
                        " public void setName(String name);",
                        "}")
                .addFormattedJava("my.FooComponent",
                        "package my;",
                        "import com.atlassian.activeobjects.external.*;",
                        "public class FooComponent implements it.com.atlassian.activeobjects.ActiveObjectsTestConsumer, TransactionCallback {",
                        "  ActiveObjects mgr;",
                        "  public FooComponent(ActiveObjects mgr) throws Exception {",
                        "    this.mgr = mgr;",
                        "  }",
                        "  public void init() throws Exception {",
                        "    mgr.migrate(new Class[]{my.Foo.class});",
                        "  }",
                        "  public Object run() throws Exception {",
                        "    return mgr.executeInTransaction(this);",
                        "  }",
                        "  public Object doInTransaction(TransactionStatus status) throws java.sql.SQLException {",
                        "    Foo foo = (Foo) mgr.create(my.Foo.class, new net.java.ao.DBParam[0]);",
                        "    foo.setName('bob');",
                        "    foo.save();",
                        "    foo = (Foo) mgr.find(Foo.class, 'id = ?', new Object[]{foo.getID()})[0];",
                        "    if (foo == null) throw new RuntimeException('no foo found');",
                        "    if (foo.getName() == null) throw new RuntimeException('foo has no name');",
                        "    if (!foo.getName().equals('bob')) throw new RuntimeException('foo name wrong');",
                        "    return null;",
                        "  }",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='" + key + "' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='obj' class='my.FooComponent' public='true' interface='it.com.atlassian.activeobjects.ActiveObjectsTestConsumer' />",
                        "    <component-import key='emp' interface='com.atlassian.activeobjects.external.ActiveObjects' />",
                        "</atlassian-plugin>")
                .build();
    }

    private File buildConfigPlugin(String path) throws Exception
    {
        return new PluginJarBuilder()
                .addFormattedJava("config.MyConfig",
                        "package config;",
                        "public class MyConfig implements com.atlassian.activeobjects.external.ActiveObjectsConfiguration {",
                        "  public String getDatabaseBaseDirectory() { return '" + path + "'; }",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='testconfig.ao.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='obj' class='config.MyConfig' public='true' interface='com.atlassian.activeobjects.external.ActiveObjectsConfiguration' />",
                        "</atlassian-plugin>")
                .build();
    }
}
