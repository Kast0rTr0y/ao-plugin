package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.TenantProvider;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.ExecutorServiceProvider;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.tenancy.api.Tenant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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
    private DataSourceProvider dataSourceProvider;
    
    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TenantProvider tenantProvider;

    @Mock
    private AOConfigurationGenerator aoConfigurationGenerator;

    @Mock
    private ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    @Mock
    private ExecutorServiceProvider executorServiceProvider;

    private final Tenant tenant1 = mock(Tenant.class);
    private final Tenant tenant2 = mock(Tenant.class);
    private final ExecutorService executorService1 = mock(ExecutorService.class);
    private final ExecutorService executorService2 = mock(ExecutorService.class);
    private final Bundle bundle1 = mock(Bundle.class);
    private final Bundle bundle2 = mock(Bundle.class);
    private final BabyBearActiveObjectsDelegate babyBear1 = mock(BabyBearActiveObjectsDelegate.class);
    private final BabyBearActiveObjectsDelegate babyBear2 = mock(BabyBearActiveObjectsDelegate.class);

    @Before
    public void setUp() throws Exception
    {
        when(threadLocalDelegateExecutorFactory.createScheduledExecutorService(any(ScheduledExecutorService.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArguments()[0];
            }
        });

        serviceFactory = new ActiveObjectsServiceFactory(factory, eventPublisher, dataSourceProvider,
                transactionTemplate, tenantProvider, aoConfigurationGenerator, threadLocalDelegateExecutorFactory,
                executorServiceProvider);

        assertThat(serviceFactory.aoContextThreadFactory, is(ContextClassLoaderThreadFactory.class));
        assertSame(((ContextClassLoaderThreadFactory) serviceFactory.aoContextThreadFactory).getContextClassLoader(), Thread.currentThread().getContextClassLoader());
        assertThat(serviceFactory.configExecutor, notNullValue());

        verify(threadLocalDelegateExecutorFactory).createScheduledExecutorService(any(ScheduledExecutorService.class));
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

        assertThat(serviceFactory.configExecutor.isShutdown(), is(true));

        verify(eventPublisher).unregister(serviceFactory);
        verify(executorService1).shutdown();
        verify(executorService2).shutdown();
    }

    @Test
    public void initExecutorFn()
    {
        serviceFactory.initExecutorsByTenant.put(tenant1, executorService1);
        serviceFactory.initExecutorsByTenant.put(tenant2, executorService2);

        assertSame(serviceFactory.initExecutorFn.apply(tenant1), executorService1);
    }

    @Test
    public void initExecutorsLoading()
    {
        when(threadLocalDelegateExecutorFactory.createExecutorService(any(ExecutorService.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArguments()[0];
            }
        });

        assertThat(serviceFactory.initExecutorFn.apply(tenant1), is(ExecutorService.class));

        verify(threadLocalDelegateExecutorFactory).createExecutorService(any(ExecutorService.class));
    }

    @Test
    public void initExecutorsLoadingSnowflake()
    {
        final ExecutorService snowflakeExecutorService = mock(ExecutorService.class);

        when(executorServiceProvider.initExecutorService()).thenReturn(snowflakeExecutorService);

        assertSame(serviceFactory.initExecutorFn.apply(tenant1), snowflakeExecutorService);

        verify(executorServiceProvider).initExecutorService();
    }

    @Test
    public void getService()
    {
        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);
        serviceFactory.aoDelegatesByBundle.put(bundle2, babyBear2);

        assertSame(serviceFactory.getService(bundle1, null), babyBear1);
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

        when(tenantProvider.getTenant()).thenReturn(tenant1);
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

        when(tenantProvider.getTenant()).thenReturn(tenant1);
        when(babyBear1.getBundle()).thenReturn(bundle1);
        when(babyBear2.getBundle()).thenReturn(bundle2);
        when(bundle1.getSymbolicName()).thenReturn("bundle1");
        when(bundle2.getSymbolicName()).thenReturn("bundle2");

        serviceFactory.onHotRestart(null);

        verify(babyBear1).restartActiveObjects(tenant1);
        verify(babyBear2).restartActiveObjects(tenant1);
    }
}
