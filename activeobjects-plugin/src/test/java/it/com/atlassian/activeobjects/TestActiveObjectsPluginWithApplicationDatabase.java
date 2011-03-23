package it.com.atlassian.activeobjects;

import com.atlassian.activeobjects.internal.ActiveObjectsSettingKeys;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import org.hsqldb.jdbc.jdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.File;

import static com.atlassian.activeobjects.test.ActiveObjectsAssertions.*;
import static com.atlassian.activeobjects.test.Plugins.*;
import static org.mockito.Mockito.*;

public final class TestActiveObjectsPluginWithApplicationDatabase extends BaseActiveObjectsIntegrationTest
{
    private static final String CONSUMER_PLUGIN_KEY = "ao-test-consumer";

    private File applicationDatabaseDirectory;

    @Before
    public final void setUp()
    {
        // plugin settings
        final PluginSettings globalSettings = mock(PluginSettings.class);
        when(globalSettings.get(endsWith(ActiveObjectsSettingKeys.DATA_SOURCE_TYPE))).thenReturn(DataSourceType.APPLICATION.name());
        when(globalSettings.get(endsWith(ActiveObjectsSettingKeys.MODEL_VERSION))).thenReturn("0");
        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(globalSettings);

        applicationDatabaseDirectory = folder.newFolder("application-db");

        when(dataSourceProvider.getDataSource()).thenReturn(hsqlDataSource(applicationDatabaseDirectory, CONSUMER_PLUGIN_KEY));

        container.start();
    }

    @After
    public final void tearDown()
    {
        container.stop();
    }

    @Test
    public final void databaseCreatedInApplicationDatabaseDirectory() throws Exception
    {
        container.install(newConsumerPlugin(CONSUMER_PLUGIN_KEY));
        container.getService(ActiveObjectsTestConsumer.class).run();

        assertDatabaseExists(applicationDatabaseDirectory, CONSUMER_PLUGIN_KEY);
    }

    private static DataSource hsqlDataSource(File applicationDatabaseDirectory, String dbName)
    {
        final jdbcDataSource hsqlDs = new jdbcDataSource();
        hsqlDs.setUser("sa");
        hsqlDs.setPassword("");
        hsqlDs.setDatabase(
                new StringBuilder().append("jdbc:hsqldb:file:")
                        .append(applicationDatabaseDirectory)
                        .append("/").append(dbName).append("/db;hsqldb.default_table_type=cached").toString());
        return hsqlDs;
    }
}
