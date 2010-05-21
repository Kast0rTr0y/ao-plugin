package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestRegistryBasedActiveObjectsProvider
{
    private static final String PLUGIN_KEY_1 = "foo";
    private static final String PLUGIN_KEY_2 = "bar";

    private ActiveObjectsProvider provider;
    @Mock
    private ActiveObjectsRegistry registry;
    @Mock
    private ActiveObjectsFactoryResolver resolver;
    @Mock
    private ActiveObjects activeObjects;
    @Mock
    private ActiveObjectsFactory activeObjectsFactory;

    @Before
    public void setUp() throws Exception
    {
        provider = new RegistryBasedActiveObjectsProvider(registry, resolver);

        when(registry.get(PLUGIN_KEY_1)).thenReturn(activeObjects);
        when(registry.get(PLUGIN_KEY_2)).thenReturn(null);
        when(registry.register(anyString(), eq(activeObjects))).thenReturn(activeObjects);

        when(resolver.get(PLUGIN_KEY_2)).thenReturn(activeObjectsFactory);
        when(activeObjectsFactory.create()).thenReturn(activeObjects);

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

        verify(resolver).get(PLUGIN_KEY_2);
        verify(activeObjectsFactory).create();
    }
}
