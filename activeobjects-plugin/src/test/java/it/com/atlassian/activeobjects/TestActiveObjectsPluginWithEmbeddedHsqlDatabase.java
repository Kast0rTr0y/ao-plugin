package it.com.atlassian.activeobjects;

import com.atlassian.activeobjects.internal.ActiveObjectsSettingKeys;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginException;
import com.atlassian.sal.api.component.ComponentLocator;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.atlassian.activeobjects.test.ActiveObjectsAssertions.assertDatabaseExists;
import static com.atlassian.activeobjects.test.Plugins.newConfigurationPlugin;
import static com.atlassian.activeobjects.test.Plugins.newConsumerPlugin;
import static org.mockito.Mockito.*;

public final class TestActiveObjectsPluginWithEmbeddedHsqlDatabase extends BaseActiveObjectsIntegrationTest
{
    private static final String CONSUMER_PLUGIN_KEY = "ao-test-consumer";
    private static final String CONFIGURATION_PLUGIN_KEY = "ao-config-1";

    private File homeDirectory;

    @Before
    public final void setUp()
    {
        ComponentLocator componentLocator = mock(ComponentLocator.class);
        ComponentLocator.setComponentLocator(componentLocator);
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

        uninstallPlugin(configPlugin);
        container.install(newConfigurationPlugin(CONFIGURATION_PLUGIN_KEY, "foo2"));

        // one must re-start a plugin to pick up the new configuration
        uninstallPlugin(installedConsumerPlugin);
        container.install(consumerPluginFile);

        container.getService(ActiveObjectsTestConsumer.class).run();
        assertDatabaseExists(homeDirectory, "foo2", CONSUMER_PLUGIN_KEY);
    }

    private void uninstallPlugin(Plugin configPlugin)
    {
        try
        {
            container.unInstall(configPlugin);
        }
        catch (PluginException e)
        {
            //ignore the unable to delete file failure since it might happen on windows
            if (!e.getMessage().contains("Unable to delete file"))
            {
                Assert.fail(e.getMessage());
            }
        }
    }
}
