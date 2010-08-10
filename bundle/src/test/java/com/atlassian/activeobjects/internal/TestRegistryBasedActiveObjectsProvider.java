package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link com.atlassian.activeobjects.internal.RegistryBasedActiveObjectsProvider}
 */
@RunWith(MockitoJUnitRunner.class)
public class TestRegistryBasedActiveObjectsProvider
{
    private static final PluginKey PLUGIN_KEY_1 = new PluginKey("foo");
    private static final PluginKey PLUGIN_KEY_2 = new PluginKey("bar");
    private static final DataSourceType PLUGIN_KEY_2_DATA_SOURCE_TYPE = DataSourceType.APPLICATION;

    private ActiveObjectsProvider provider;
    @Mock
    private ActiveObjectsRegistry registry;
    @Mock
    private ActiveObjects activeObjects;
    @Mock
    private ActiveObjectsFactory activeObjectsFactory;
    @Mock
    private DataSourceTypeResolver dataSourceTypeResolver;

    @Before
    public void setUp() throws Exception
    {

        provider = new RegistryBasedActiveObjectsProvider(registry, activeObjectsFactory, dataSourceTypeResolver);

        when(registry.get(PLUGIN_KEY_1)).thenReturn(activeObjects);
        when(registry.get(PLUGIN_KEY_2)).thenReturn(null);
        when(registry.register(Matchers.<PluginKey>anyObject(), eq(activeObjects))).thenReturn(activeObjects);
        when(dataSourceTypeResolver.getDataSourceType(PLUGIN_KEY_2)).thenReturn(PLUGIN_KEY_2_DATA_SOURCE_TYPE);

        when(activeObjectsFactory.create(PLUGIN_KEY_2_DATA_SOURCE_TYPE, PLUGIN_KEY_2)).thenReturn(activeObjects);
    }

    @Test
    public void testGetExistingActiveObjectsReturnsSameInstance()
    {
        assertEquals(activeObjects, provider.get(PLUGIN_KEY_1));
        assertEquals(activeObjects, provider.get(PLUGIN_KEY_1));
    }

    @Test
    public void testGetNonExistingActiveObjectReturnsNewInstance()
    {
        assertEquals(activeObjects, provider.get(PLUGIN_KEY_2));

        verify(activeObjectsFactory).create(PLUGIN_KEY_2_DATA_SOURCE_TYPE, PLUGIN_KEY_2);
    }
}
