package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
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
    private ActiveObjectsProvider provider;

    @Mock
    private ActiveObjectsConfiguration configuration1;
    @Mock
    private ActiveObjectsConfiguration configuration2;
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
        provider = new RegistryBasedActiveObjectsProvider(registry, activeObjectsFactory);

        when(registry.get(configuration1)).thenReturn(activeObjects);
        when(registry.get(configuration2)).thenReturn(null);
        when(registry.register(anyActiveObjectsConfiguration(), eq(activeObjects))).thenReturn(activeObjects);
        when(activeObjectsFactory.create(configuration2)).thenReturn(activeObjects);
    }

    @Test
    public void testGetExistingActiveObjectsReturnsSameInstance()
    {
        assertEquals(activeObjects, provider.get(configuration1));
        assertEquals(activeObjects, provider.get(configuration1));
    }

    @Test
    public void testGetNonExistingActiveObjectReturnsNewInstance()
    {
        assertEquals(activeObjects, provider.get(configuration2));
        verify(activeObjectsFactory).create(configuration2);
    }

    private ActiveObjectsConfiguration anyActiveObjectsConfiguration()
    {
        return Matchers.anyObject();
    }
}
