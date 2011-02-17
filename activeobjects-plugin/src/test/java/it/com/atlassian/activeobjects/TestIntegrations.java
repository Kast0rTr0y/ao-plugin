package it.com.atlassian.activeobjects;


import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.core.transaction.NoOpTransactionTemplate;
import org.hsqldb.jdbc.jdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.util.tracker.ServiceTracker;

import javax.sql.DataSource;
import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.atlassian.activeobjects.test.IntegrationTestHelper.*;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the active objects plugin
 */
public class TestIntegrations extends PluginInContainerTestBase
{
    private File homeDirectory;
    private ApplicationProperties applicationProperties;

    /**
     * an atomic boolean to simulate the system being down, i.e. some services not being available anymore
     *
     * @see #getHostComponentProvider()
     */
    private AtomicBoolean isSystemDown;
    private PluginSettings pluginSettings;
    private DataSource dataSource;
    private File applicationDbDir;

    @Before
    public void onSetUp() throws Exception
    {
        homeDirectory = getTmpDir(getClass().getName());
        applicationProperties = getMockApplicationProperties();
        pluginSettings = mock(PluginSettings.class);
        dataSource = mockDataSource();
        applicationDbDir = getTmpDir("application_db");

        isSystemDown = new AtomicBoolean(false);
    }

    private DataSource mockDataSource() throws SQLException
    {
        final DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDriverName()).thenReturn("hsql");
        return dataSource;
    }

    @After
    public void onTearDown() throws Exception
    {
        deleteDirectory(homeDirectory);
        deleteDirectory(applicationDbDir);

        isSystemDown = null;
        homeDirectory = null;
        applicationProperties = null;
        pluginSettings = null;
        dataSource = null;
        applicationDbDir = null;
    }

    @Test
    public void testWithHsqlDatabaseInDefaultDirectoryWithinHomeDirectory() throws Exception
    {
        setDataSourceType(DataSourceType.HSQLDB);

        final ServiceTracker tracker = initPluginManagerWithActiveObjects(ActiveObjectsTestConsumer.class);
        installConsumerPlugin();

        callActiveObjectsConsumer(tracker);
        assertDatabaseExists(homeDirectory, "data/plugins/activeobjects", "test-");
    }

    @Test
    public void testWithDataSourceProvidedByApplication() throws Exception
    {
        setDataSourceType(DataSourceType.APPLICATION);

        final ServiceTracker tracker = initPluginManagerWithActiveObjects(ActiveObjectsTestConsumer.class);
        installConsumerPlugin();

        callActiveObjectsConsumer(tracker);
        assertDatabaseExists(applicationDbDir, "test-");
    }

    @Test
    public void testWithHsqlDatabaseInConfiguredDirectoryWithinHomeDirectory() throws Exception
    {
        setDataSourceType(DataSourceType.HSQLDB);

        final ServiceTracker tracker = initPluginManagerWithActiveObjects(ActiveObjectsTestConsumer.class);

        // the plugin that configures the database in a specific directory
        final String configPluginKey = installPlugin(buildConfigPlugin("foo"));
        installConsumerPlugin();

        callActiveObjectsConsumer(tracker);
        assertDatabaseExists(homeDirectory, "foo", "test-");

        uninstallPlugin(configPluginKey);

        installPlugin(buildConfigPlugin("foo2"));

        callActiveObjectsConsumer(tracker);
        assertDatabaseExists(homeDirectory, "foo2", "test-");
    }

    @Test
    public void testClientSurvivesRequiredDepChange() throws Exception
    {
        setDataSourceType(DataSourceType.HSQLDB);

        final File childDir = getDir(homeDirectory, "child");

        final ServiceTracker tracker = initPluginManagerWithActiveObjects(ActiveObjectsTestConsumer.class);
        installConsumerPlugin();

//        callActiveObjectsConsumer(tracker); TODO should that line be part of the test???
        assertDatabaseDoesNotExists(childDir, "data/plugins/activeobjects", "test-");

        // updating the home directory
        when(applicationProperties.getHomeDirectory()).thenReturn(childDir);

        pluginManager.warmRestart();

        callActiveObjectsConsumer(tracker);
        assertDatabaseExists(childDir, "data/plugins/activeobjects", "test-");
    }

    /**
     * Here system is down is simulated by removing some necessary services to Active Objects
     *
     * @throws Exception whatever
     */
    @Test
    public void testActiveObjectsConsumerWhenSystemIsDown() throws Exception
    {
        setDataSourceType(DataSourceType.HSQLDB);

        final ServiceTracker tracker = initPluginManagerWithActiveObjects(ActiveObjectsTestConsumer.class);
        installConsumerPlugin();

        isSystemDown.set(true);
        pluginManager.warmRestart();

        long start = System.currentTimeMillis();
        try
        {
            callActiveObjectsConsumer(tracker);
            fail("Should have thrown an exception");
        }
        catch (RuntimeException e)
        {
            assertEquals("service matching filter=[(objectClass=com.atlassian.sal.api.ApplicationProperties)] unavailable", e.getMessage());
            assertTrue(System.currentTimeMillis() - (start + 3000) > 0);
        }
    }

    @Test
    public void testBasicWithLotsOfConcurrentCalls() throws Exception
    {
        setDataSourceType(DataSourceType.HSQLDB);

        final ServiceTracker tracker = initPluginManagerWithActiveObjects(ActiveObjectsTestConsumer.class);

        installConsumerPlugin();

        final ActiveObjectsTestConsumer runnable = (ActiveObjectsTestConsumer) tracker.waitForService(10000);
        final AtomicBoolean failFlag = new AtomicBoolean(false);
        Runnable r = new Runnable()
        {
            int count = 0;

            public void run()
            {
                if (!failFlag.get())
                {
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
        assertDatabaseExists(homeDirectory, "data/plugins/activeobjects", "test-");
    }

    private ServiceTracker initPluginManagerWithActiveObjects(final Class<?> serviceToTrack) throws Exception
    {
        initPluginManager(getHostComponentProvider());
        installActiveObjectsPlugin();
        return getServiceTracker(serviceToTrack);
    }

    private String installActiveObjectsPlugin()
    {
        return installPlugin(getPluginJar());
    }

    private void installConsumerPlugin() throws Exception
    {
        installPlugin(buildConsumerPlugin("test-consumer"));
    }

    private String installPlugin(File plugin)
    {
        final String pluginKey = pluginManager.installPlugin(new JarPluginArtifact(plugin));
        assertTrue(pluginManager.isPluginEnabled(pluginKey));
        return pluginKey;
    }

    private void uninstallPlugin(String configPluginKey)
    {
        pluginManager.uninstall(pluginManager.getPlugin(configPluginKey));
    }

    private HostComponentProvider getHostComponentProvider()
    {
        return new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                if (!isSystemDown.get())
                {
                    registrar.register(ApplicationProperties.class).forInstance(applicationProperties);
                }
                registrar.register(TransactionTemplate.class).forInstance(new NoOpTransactionTemplate());
                registrar.register(PluginSettingsFactory.class).forInstance(getMockPluginSettingsFactory());
                registrar.register(DataSourceProvider.class).forInstance(getMockDataSourceProvider());
                registrar.register(I18nResolver.class).forInstance(mock(I18nResolver.class));
                registrar.register(UserManager.class).forInstance(mock(UserManager.class));
                registrar.register(LoginUriProvider.class).forInstance(mock(LoginUriProvider.class));
                registrar.register(WebResourceManager.class).forInstance(mock(WebResourceManager.class));
                registrar.register(WebInterfaceManager.class).forInstance(mock(WebInterfaceManager.class));
                registrar.register(PluginAccessor.class).forInstance(mock(PluginAccessor.class));
            }
        };
    }

    private PluginSettingsFactory getMockPluginSettingsFactory()
    {
        final PluginSettingsFactory pluginSettingsFactory = mock(PluginSettingsFactory.class);
        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);
        return pluginSettingsFactory;
    }

    private void setDataSourceType(DataSourceType type)
    {
        when(pluginSettings.get(anyString())).thenReturn(type.name());
        if (DataSourceType.APPLICATION.equals(type))
        {
            final jdbcDataSource hsqlDs = new jdbcDataSource();
            hsqlDs.setDatabase(getApplicationDataSourceUrl());
            hsqlDs.setUser("sa");
            hsqlDs.setPassword("");
            dataSource = hsqlDs;
        }
    }

    private String getApplicationDataSourceUrl()
    {
        return "jdbc:hsqldb:file:" + applicationDbDir + "" + "/test-application/db;hsqldb.default_table_type=cached";
    }

    private ApplicationProperties getMockApplicationProperties()
    {
        final ApplicationProperties properties = mock(ApplicationProperties.class);
        when(properties.getHomeDirectory()).thenReturn(homeDirectory);
        return properties;
    }

    private DataSourceProvider getMockDataSourceProvider()
    {
        final DataSourceProvider dataSourceProvider = mock(DataSourceProvider.class);
        when(dataSourceProvider.getDataSource()).thenReturn(dataSource);
        return dataSourceProvider;
    }


    private void callActiveObjectsConsumer(ServiceTracker tracker) throws Exception
    {
        ActiveObjectsTestConsumer runnable = (ActiveObjectsTestConsumer) tracker.waitForService(10000);
        runnable.run();
    }

    private void assertDatabaseExists(File dbDir, String dbPrefix)
    {
        assertDatabaseExists(dbDir, dbPrefix, 1);
    }

    private void assertDatabaseExists(File baseDir, String path, final String dbprefix)
    {
        assertDatabaseExists(new File(baseDir, path), dbprefix, 1);
    }

    private void assertDatabaseDoesNotExists(File baseDir, String path, final String dbprefix)
    {
        assertDatabaseExists(new File(baseDir, path), dbprefix, 0);
    }

    private void assertDatabaseExists(File dbDir, final String dbprefix, int expected)
    {
        final File[] files = dbDir.listFiles(new FilenameFilter()
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
                        "public class FooComponent implements it.com.atlassian.activeobjects.ActiveObjectsTestConsumer, com.atlassian.sal.api.transaction.TransactionCallback {",
                        "  ActiveObjects mgr;",
                        "  public FooComponent(ActiveObjects mgr) throws Exception {",
                        "    this.mgr = mgr;",
                        "  }",
                        "  public Object run() throws Exception {",
                        "    return mgr.executeInTransaction(this);",
                        "  }",
                        "  public Object doInTransaction() {",
                        "    try {",
                        "        Foo foo = (Foo) mgr.create(my.Foo.class, new net.java.ao.DBParam[0]);",
                        "        foo.setName('bob');",
                        "        foo.save();",
                        "        foo = (Foo) mgr.find(Foo.class, 'id = ?', new Object[]{foo.getID()})[0];",
                        "        if (foo == null) throw new RuntimeException('no foo found');",
                        "        if (foo.getName() == null) throw new RuntimeException('foo has no name');",
                        "        if (!foo.getName().equals('bob')) throw new RuntimeException('foo name wrong');",
                        "        return null;",
                        "    } catch (java.sql.SQLException e) { throw new RuntimeException(e); }",
                        "  }",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='" + key + "' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='obj' class='my.FooComponent' public='true' interface='it.com.atlassian.activeobjects.ActiveObjectsTestConsumer' />",
                        "    <ao key='ao'>",
                        "        <entity>my.Foo</entity>",
                        "    </ao>",
                        "    <component-import key='emp' interface='com.atlassian.activeobjects.external.ActiveObjects' />",
                        "</atlassian-plugin>")
                .build();
    }

    private File buildConfigPlugin(String path) throws Exception
    {
        return new PluginJarBuilder()
                .addFormattedJava("config.MyConfig",
                        "package config;",
                        "public class MyConfig implements com.atlassian.activeobjects.spi.ActiveObjectsPluginConfiguration {",
                        "  public String getDatabaseBaseDirectory() { return '" + path + "'; }",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='testconfig.ao.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='obj' class='config.MyConfig' public='true' interface='com.atlassian.activeobjects.spi.ActiveObjectsPluginConfiguration' />",
                        "</atlassian-plugin>")
                .build();
    }
}
