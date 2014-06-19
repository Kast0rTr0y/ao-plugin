package it.com.atlassian.activeobjects;

import com.atlassian.activeobjects.internal.ActiveObjectsSettingKeys;
import com.atlassian.activeobjects.internal.DataSourceType;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.atlassian.activeobjects.test.ActiveObjectsAssertions.*;
import static com.atlassian.activeobjects.test.Plugins.*;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public final class TestActiveObjectsPluginConcurrency extends BaseActiveObjectsIntegrationTest
{
    private static final String CONSUMER_PLUGIN_KEY = "ao-test-consumer";

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
    public final void lotsOfConcurrentCalls() throws Exception
    {
        container.install(newConsumerPlugin(CONSUMER_PLUGIN_KEY));
        final ActiveObjectsTestConsumer consumer = container.getService(ActiveObjectsTestConsumer.class);

        final AtomicBoolean failFlag = new AtomicBoolean(false);
        final Runnable r = new Runnable()
        {
            public void run()
            {
                if (!failFlag.get())
                {
                    try
                    {
                        consumer.run();
                    }
                    catch (Exception e)
                    {
                        failFlag.set(true);
                        e.printStackTrace();
                    }
                }
            }
        };

        execute(r, 1000, 10);

        assertFalse(failFlag.get());
        assertDatabaseExists(homeDirectory, "data/plugins/activeobjects", CONSUMER_PLUGIN_KEY);
    }

    private void execute(Runnable r, int numberOfExecutions, int sizeOfPool) throws InterruptedException
    {
        final ExecutorService executor = Executors.newFixedThreadPool(sizeOfPool);
        for (int x = 0; x < numberOfExecutions; x++)
        {
            executor.execute(r);
        }
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
    }
}
