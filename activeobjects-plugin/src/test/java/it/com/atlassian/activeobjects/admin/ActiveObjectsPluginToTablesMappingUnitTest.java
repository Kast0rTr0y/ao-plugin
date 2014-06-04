package it.com.atlassian.activeobjects.admin;


import com.atlassian.activeobjects.admin.ActiveObjectsPluginToTablesMapping;
import com.atlassian.activeobjects.admin.PluginToTablesMapping;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith (MockitoJUnitRunner.class)
public class ActiveObjectsPluginToTablesMappingUnitTest
{
    private ActiveObjectsPluginToTablesMapping aoPluginToTablesMapping;

    @Mock
    private PluginSettingsFactory pluginSettingsFactory;

    @Mock
    private PluginSettings pluginSettings;

    @Mock
    private Map<String, PluginToTablesMapping.PluginInfo> mappingFromSettings;

    private final class ActiveObjectsPluginToTablesMappingStubbed extends ActiveObjectsPluginToTablesMapping
    {
        public ActiveObjectsPluginToTablesMappingStubbed(final PluginSettingsFactory factory)
        {
            super(factory);
        }

        @Override
        protected Map<String, PluginInfo> getMappingFromSettings()
        {
            return mappingFromSettings;
        }
    }

    @Before
    public void setUp() throws Exception
    {
        when(pluginSettingsFactory.createGlobalSettings()).thenReturn(pluginSettings);

        aoPluginToTablesMapping = new ActiveObjectsPluginToTablesMappingStubbed(pluginSettingsFactory);

        verify(pluginSettingsFactory).createGlobalSettings();
    }

    /**
     * Ensure that we always hit the store when retrieving i.e. no caching
     */
    @Test
    public void getAlwaysHitsStore()
    {
        PluginToTablesMapping.PluginInfo pluginInfo1 = mock(PluginToTablesMapping.PluginInfo.class);
        PluginToTablesMapping.PluginInfo pluginInfo2 = mock(PluginToTablesMapping.PluginInfo.class);

        when(mappingFromSettings.get("table1")).thenReturn(pluginInfo1);
        when(mappingFromSettings.get("table2")).thenReturn(pluginInfo2);

        assertThat(aoPluginToTablesMapping.get("table1"), is(pluginInfo1));
        assertThat(aoPluginToTablesMapping.get("table2"), is(pluginInfo2));

        verify(mappingFromSettings).get("table1");
        verify(mappingFromSettings).get("table2");
    }

    /**
     * Ensure that we always retrieve the store before adding i.e. add to what's in there rather than rewriting an old
     * cache
     */
    @Test
    public void addAlwaysHitsStore()
    {
        PluginToTablesMapping.PluginInfo pluginInfo1 = mock(PluginToTablesMapping.PluginInfo.class);
        PluginToTablesMapping.PluginInfo pluginInfo2 = mock(PluginToTablesMapping.PluginInfo.class);

        mappingFromSettings = new HashMap<String, PluginToTablesMapping.PluginInfo>();
        mappingFromSettings.put("table1", pluginInfo1);

        aoPluginToTablesMapping.add(pluginInfo2, ImmutableList.of("table2"));

        assertThat(mappingFromSettings, hasEntry("table1", pluginInfo1));
        assertThat(mappingFromSettings, hasEntry("table2", pluginInfo2));

        verify(pluginSettings).put(ActiveObjectsPluginToTablesMapping.class.getName(), new Gson().toJson(mappingFromSettings));
    }
}
