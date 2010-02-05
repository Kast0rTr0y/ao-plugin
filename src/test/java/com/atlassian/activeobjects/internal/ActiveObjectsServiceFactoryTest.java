package com.atlassian.activeobjects.internal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActiveObjectsServiceFactoryTest
{
    private ActiveObjectsServiceFactory serviceFactory;

    @Mock
    private ActiveObjectsProvider provider;
    @Mock
    private PluginKeyFactory keyFactory;

    @Before
    public void setUp() throws Exception
    {
        serviceFactory = new ActiveObjectsServiceFactory(provider, keyFactory);
    }

    @Test
    public void testGetService()
    {
        final Bundle bundle = mock(Bundle.class);
        when(keyFactory.get(bundle)).thenReturn("a-key");

        final Object ao = serviceFactory.getService(bundle, null); // the service registration is not used
        assertNotNull(ao);
        assertTrue(ao instanceof DelegatingActiveObjects);

        assertEquals("a-key", ((DelegatingActiveObjects) ao).getPluginKey());
        assertEquals(provider, ((DelegatingActiveObjects) ao).getProvider());
    }

    @Test
    public void testUnGetService()
    {
        serviceFactory.ungetService(null, null, null);
        verifyZeroInteractions(provider, keyFactory);
    }
}
