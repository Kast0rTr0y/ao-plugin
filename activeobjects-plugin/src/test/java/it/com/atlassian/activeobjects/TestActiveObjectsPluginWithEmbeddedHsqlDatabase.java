package it.com.atlassian.activeobjects;

import com.atlassian.activeobjects.internal.ActiveObjectsSettingKeys;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.plugin.Plugin;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.atlassian.activeobjects.test.ActiveObjectsAssertions.*;
import static com.atlassian.activeobjects.test.Plugins.*;
import static org.mockito.Mockito.*;

public final class TestActiveObjectsPluginWithEmbeddedHsqlDatabase extends BaseActiveObjectsIntegrationTest
{
    private static final String CONSUMER_PLUGIN_KEY = "ao-test-consumer";
    private static final String CONFIGURATION_PLUGIN_KEY = "ao-config-1";

    private File homeDirectory;

    @Before
    public final void setUp()
    {
        // plugin settings
        final PluginSettings globalSettings = mock(PluginSettings.class);
        when(globalSettings.get(endsWith(ActiveObjectsSettingKeys.DATA_SOURCE_TYPE))).thenReturn(DataSourceType.HSQLDB.name());
        when(globalSettings.get(endsWith(ActiveObjectsSettingKeys.MODEL_VERSION))).thenReturn("0");
        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(globalSettings);

        // home directory
        homeDirectory = folder.newFolder("home-directory");
        when(applicationProperties.getHomeDirectory()).thenReturn(homeDirectory);

        container.start();
    }

    @After
    public final void tearDown()
    {
        container.stop();
    }

    @Test
    public final void databaseCreatedInDefaultDirectoryWithinHomeDirectory() throws Exception
    {
        container.install(newConsumerPlugin(CONSUMER_PLUGIN_KEY));
        container.getService(ActiveObjectsTestConsumer.class).run();

        assertDatabaseExists(homeDirectory, "data/plugins/activeobjects", CONSUMER_PLUGIN_KEY);
    }

    @Test
    public void databaseCreatedInConfiguredDirectoryWithinHomeDirectory() throws Exception
    {
        final Plugin configPlugin = container.install(newConfigurationPlugin(CONFIGURATION_PLUGIN_KEY, "foo"));
        File consumerPluginFile = newConsumerPlugin(CONSUMER_PLUGIN_KEY);
        Plugin installedConsumerPlugin = container.install(consumerPluginFile);

        container.getService(ActiveObjectsTestConsumer.class).run();

        assertDatabaseExists(homeDirectory, "foo", CONSUMER_PLUGIN_KEY);

        container.unInstall(configPlugin);
        container.install(newConfigurationPlugin(CONFIGURATION_PLUGIN_KEY, "foo2"));

        // one must re-start a plugin to pick up the new configuration
        container.unInstall(installedConsumerPlugin);
        container.install(consumerPluginFile);

        container.getService(ActiveObjectsTestConsumer.class).run();
        assertDatabaseExists(homeDirectory, "foo2", CONSUMER_PLUGIN_KEY);
    }
}
