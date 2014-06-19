package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.spi.ContextClassLoaderThreadFactory;
import com.atlassian.activeobjects.spi.InitExecutorServiceProvider;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.tenancy.api.TenantContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class ActiveObjectsServiceFactoryTest
{
    private ActiveObjectsServiceFactory serviceFactory;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
    private TenantContext tenantContext;

    @Mock
    private ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    @Mock
    private InitExecutorServiceProvider initExecutorServiceProvider;

    private final Tenant tenant1 = mock(Tenant.class);
    private final Tenant tenant2 = mock(Tenant.class);
    private final ExecutorService executorService1 = mock(ExecutorService.class);
    private final ExecutorService executorService2 = mock(ExecutorService.class);
    private final Bundle bundle1 = mock(Bundle.class);
    private final Bundle bundle2 = mock(Bundle.class);
    private final TenantAwareActiveObjects babyBear1 = mock(TenantAwareActiveObjects.class);
    private final TenantAwareActiveObjects babyBear2 = mock(TenantAwareActiveObjects.class);

    @Before
    public void setUp() throws Exception
    {
        serviceFactory = new ActiveObjectsServiceFactory(factory, eventPublisher, tenantContext,
                threadLocalDelegateExecutorFactory, initExecutorServiceProvider);

        assertThat(serviceFactory.aoContextThreadFactory, is(ContextClassLoaderThreadFactory.class));
        assertThat(((ContextClassLoaderThreadFactory) serviceFactory.aoContextThreadFactory).getContextClassLoader(), sameInstance(Thread.currentThread().getContextClassLoader()));
        assertThat(serviceFactory.initExecutorsShutdown, is(false));
    }

    @Test
    public void afterPropertiesSet() throws Exception
    {
        serviceFactory.afterPropertiesSet();

        verify(eventPublisher).register(serviceFactory);
    }

    @Test
    public void destroy() throws Exception
    {
        serviceFactory.initExecutorsByTenant.put(tenant1, executorService1);
        serviceFactory.initExecutorsByTenant.put(tenant2, executorService2);

        serviceFactory.destroy();

        assertThat(serviceFactory.initExecutorsShutdown, is(true));

        verify(eventPublisher).unregister(serviceFactory);
        verify(executorService1).shutdownNow();
        verify(executorService2).shutdownNow();
    }

    @Test
    public void initExecutorFn()
    {
        serviceFactory.initExecutorsByTenant.put(tenant1, executorService1);
        serviceFactory.initExecutorsByTenant.put(tenant2, executorService2);

        assertThat(serviceFactory.initExecutorFn.apply(tenant1), sameInstance(executorService1));
    }

    @Test
    public void initExecutorFnAfterDestroy() throws Exception
    {
        serviceFactory.destroy();

        expectedException.expect(IllegalStateException.class);

        serviceFactory.initExecutorFn.apply(tenant1);
    }

    @Test
    public void getService()
    {
        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);
        serviceFactory.aoDelegatesByBundle.put(bundle2, babyBear2);

        assertThat((TenantAwareActiveObjects) serviceFactory.getService(bundle1, null), sameInstance(babyBear1));
    }

    @Test
    public void unGetService()
    {
        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);
        serviceFactory.aoDelegatesByBundle.put(bundle2, babyBear2);

        assertThat(serviceFactory.aoDelegatesByBundle.asMap(), hasEntry(bundle1, babyBear1));
        assertThat(serviceFactory.aoDelegatesByBundle.asMap(), hasEntry(bundle2, babyBear2));
        assertThat(serviceFactory.aoDelegatesByBundle.asMap().size(), is(2));

        serviceFactory.ungetService(bundle1, null, null);

        assertThat(serviceFactory.aoDelegatesByBundle.asMap(), hasEntry(bundle2, babyBear2));
        assertThat(serviceFactory.aoDelegatesByBundle.asMap().size(), is(1));
    }

    @Test
    public void onTenantArrived()
    {
        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);
        serviceFactory.aoDelegatesByBundle.put(bundle2, babyBear2);

        when(tenantContext.getCurrentTenant()).thenReturn(tenant1);
        when(babyBear1.getBundle()).thenReturn(bundle1);
        when(babyBear2.getBundle()).thenReturn(bundle2);
        when(bundle1.getSymbolicName()).thenReturn("bundle1");
        when(bundle2.getSymbolicName()).thenReturn("bundle2");

        serviceFactory.onTenantArrived(null);

        verify(babyBear1).startActiveObjects(tenant1);
        verify(babyBear2).startActiveObjects(tenant1);
    }

    public void onHotRestart()
    {
        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);
        serviceFactory.aoDelegatesByBundle.put(bundle2, babyBear2);

        when(tenantContext.getCurrentTenant()).thenReturn(tenant1);
        when(babyBear1.getBundle()).thenReturn(bundle1);
        when(babyBear2.getBundle()).thenReturn(bundle2);
        when(bundle1.getSymbolicName()).thenReturn("bundle1");
        when(bundle2.getSymbolicName()).thenReturn("bundle2");

        serviceFactory.onHotRestart(null);

        verify(babyBear1).restartActiveObjects(tenant1);
        verify(babyBear2).restartActiveObjects(tenant1);
    }
}
