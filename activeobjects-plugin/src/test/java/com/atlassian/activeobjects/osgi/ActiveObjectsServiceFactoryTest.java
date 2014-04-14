package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.TenantProvider;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.tenancy.api.Tenant;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

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
    private ActiveObjectsConfiguration configuration;

    @Mock
    private ActiveObjectsFactory factory;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private Bundle bundle;
    
    @Mock
    private DataSourceProvider dataSourceProvider;
    
    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TenantProvider tenantProvider;

    @Mock
    private AOConfigurationServiceProvider configurationServiceProvider;

    @Mock
    private ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    @Before
    public void setUp() throws Exception
    {
        when(tenantProvider.getTenant()).thenReturn(new Tenant());

        serviceFactory = new ActiveObjectsServiceFactory(factory, eventPublisher,
                dataSourceProvider, transactionTemplate, tenantProvider, configurationServiceProvider, threadLocalDelegateExecutorFactory);
    }

    @Ignore
    @Test
    public void testGetService()
    {
        final Object ao = serviceFactory.getService(bundle, null); // the service registration is not used
        assertNotNull(ao);
        assertTrue(ao instanceof DelegatingActiveObjects);
    }

    @Ignore
    @Test
    public void testUnGetService()
    {
        Object ao = serviceFactory.getService(bundle, null);
        assertEquals(1, serviceFactory.aoInstances.size());
        serviceFactory.ungetService(bundle, null, ao);
        assertEquals(0, serviceFactory.aoInstances.size());
    }
}
