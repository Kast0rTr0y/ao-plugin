package com.atlassian.activeobjects.internal;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testing {@link DataSourceTypeResolverImpl}
 */
@RunWith(MockitoJUnitRunner.class)
public class DataSourceTypeResolverImplTest
{
    private static final Prefix PLUGIN_KEY = new SimplePrefix("bla");

    private DataSourceTypeResolver dataSourceTypeResolver;

    @Mock
    private PluginSettings pluginSettings;

    @Before
    public void setUp() throws Exception
    {
        dataSourceTypeResolver = new DataSourceTypeResolverImpl(getMockPluginSettingsFactory(), new ActiveObjectsSettingKeys(), DataSourceType.APPLICATION);
    }

    @After
    public void tearDown() throws Exception
    {
        dataSourceTypeResolver = null;
    }

    @Test
    public void testGetDataSourceTypeWithNoSettingReturnsDefaultValue() throws Exception
    {
        when(pluginSettings.get(anyString())).thenReturn(null); // no setting at all
        assertEquals(DataSourceType.APPLICATION, dataSourceTypeResolver.getDataSourceType(PLUGIN_KEY));
    }

    @Test
    public void testGetDataSourceTypeWithNonDefaultSettingReturnsSetValue() throws Exception
    {
        when(pluginSettings.get(anyString())).thenReturn(DataSourceType.HSQLDB.name());
        assertEquals(DataSourceType.HSQLDB, dataSourceTypeResolver.getDataSourceType(PLUGIN_KEY));
    }

    @Test
    public void testGetDataSourceTypeWithIncorrectSettingReturnsDefaultValue() throws Exception
    {
        when(pluginSettings.get(anyString())).thenReturn("something-that-is-not-a-datasource-type");
        assertEquals(DataSourceType.APPLICATION, dataSourceTypeResolver.getDataSourceType(PLUGIN_KEY));
    }

    private PluginSettingsFactory getMockPluginSettingsFactory()
    {
        final PluginSettingsFactory pluginSettingFactory = mock(PluginSettingsFactory.class);
        when(pluginSettingFactory.createGlobalSettings()).thenReturn(pluginSettings);
        return pluginSettingFactory;
    }
}
