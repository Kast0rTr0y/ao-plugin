package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.internal.ActiveObjectsProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class ActiveObjectsServiceFactoryTest
{
    private ActiveObjectsServiceFactory serviceFactory;

    @Mock
    private OsgiServiceUtils osgiUtils;

    @Mock
    private ActiveObjectsConfiguration configuration;

    @Mock
    private ActiveObjectsProvider provider;

    @Mock
    private Bundle bundle;

    @Before
    public void setUp() throws Exception
    {
        serviceFactory = new ActiveObjectsServiceFactory(osgiUtils, provider);

        when(osgiUtils.getService(bundle, ActiveObjectsConfiguration.class)).thenReturn(configuration);
    }

    @Test
    public void testGetService()
    {
        final Object ao = serviceFactory.getService(bundle, null); // the service registration is not used
        assertNotNull(ao);
        assertTrue(ao instanceof DelegatingActiveObjects);

        assertEquals(configuration, ((ActiveObjectsServiceFactory.LazyActiveObjectConfiguration) ((DelegatingActiveObjects) ao).getConfiguration()).getDelegate());
        assertEquals(provider, ((DelegatingActiveObjects) ao).getProvider());
    }

    @Test
    public void testUnGetService()
    {
        serviceFactory.ungetService(bundle, null, null);
        verifyZeroInteractions(provider);
    }
}
