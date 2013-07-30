package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.config.ActiveObjectsConfigurationFactory;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.TransactionSynchronisationManager;
import com.atlassian.activeobjects.util.ActiveObjectsConfigurationServiceProvider;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.transaction.TransactionTemplate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public final class ActiveObjectsServiceFactoryTest
{
    private ActiveObjectsServiceFactory serviceFactory;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private OsgiServiceUtils osgiUtils;

    @Mock
    private ActiveObjects activeObjects;

    @Mock
    private ActiveObjectsConfigurationServiceProvider configurationProvider;

    @Mock
    private ActiveObjectsConfiguration configuration;

    @Mock
    private ActiveObjectsFactory factory;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private Bundle bundle;

    @Mock
    private TransactionTemplate template;
    
    @Mock
    private TransactionSynchronisationManager tranSyncManager;
    
    @Mock
    private DataSourceProvider dataSourceProvider;
    
    @Before
    public void setUp() throws Exception
    {
        serviceFactory = new ActiveObjectsServiceFactory(factory, configurationProvider, eventPublisher, template, tranSyncManager, dataSourceProvider);

        when(osgiUtils.getService(bundle, ActiveObjectsConfiguration.class)).thenReturn(configuration);
        when(factory.create(Matchers.<ActiveObjectsConfiguration>any())).thenReturn(activeObjects);
    }

    @Test
    public void testGetService()
    {
        final Object ao = serviceFactory.getService(bundle, null); // the service registration is not used
        assertNotNull(ao);
        assertTrue(ao instanceof DelegatingActiveObjects);
    }

    @Test
    public void testUnGetService()
    {
        Object ao = serviceFactory.getService(bundle, null);
        assertEquals(1, serviceFactory.aoInstances.size());
        serviceFactory.ungetService(bundle, null, ao);
        assertEquals(0, serviceFactory.aoInstances.size());
    }
}
