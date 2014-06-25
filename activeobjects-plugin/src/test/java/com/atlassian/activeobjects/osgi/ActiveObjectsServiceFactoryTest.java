package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.plugin.ActiveObjectModuleDescriptor;
import com.atlassian.activeobjects.spi.ContextClassLoaderThreadFactory;
import com.atlassian.activeobjects.spi.InitExecutorServiceProvider;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
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

import java.util.Dictionary;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
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
    private ActiveObjectsConfiguration configuration;

    @Mock
    private ActiveObjectsFactory factory;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    @Mock
    private InitExecutorServiceProvider initExecutorServiceProvider;

    @Mock
    private Tenant tenant1;
    @Mock
    private Tenant tenant2;
    @Mock
    private ExecutorService executorService1;
    @Mock
    private ExecutorService executorService2;
    @Mock
    private Bundle bundle1;
    @Mock
    private Bundle bundle2;
    @Mock
    private Dictionary<String, String> bundle1Dictionary;
    @Mock
    private Dictionary<String, String> bundle2Dictionary;
    @Mock
    private TenantAwareActiveObjects babyBear1;
    @Mock
    private TenantAwareActiveObjects babyBear2;
    @Mock
    private PluginModuleEnabledEvent event;
    @Mock
    private ActiveObjectModuleDescriptor moduleDescriptor;
    @Mock
    private Plugin plugin1;
    @Mock
    private ActiveObjectsConfiguration aoConfig;

    @Before
    public void setUp() throws Exception
    {
        serviceFactory = new ActiveObjectsServiceFactory(factory, eventPublisher, tenantContext,
                threadLocalDelegateExecutorFactory, initExecutorServiceProvider);

        assertThat(serviceFactory.aoContextThreadFactory, is(ContextClassLoaderThreadFactory.class));
        assertThat(((ContextClassLoaderThreadFactory) serviceFactory.aoContextThreadFactory).getContextClassLoader(), sameInstance(Thread.currentThread().getContextClassLoader()));
        assertThat(serviceFactory.initExecutorsShutdown, is(false));

        when(babyBear1.getBundle()).thenReturn(bundle1);
        when(babyBear2.getBundle()).thenReturn(bundle2);
        when(bundle1.getSymbolicName()).thenReturn("bundle1");
        when(bundle2.getSymbolicName()).thenReturn("bundle2");
        when(bundle1.getHeaders()).thenReturn(bundle1Dictionary);
        when(bundle2.getHeaders()).thenReturn(bundle2Dictionary);
        when(bundle1Dictionary.get("Atlassian-Plugin-Key")).thenReturn("bundle1");
        when(bundle2Dictionary.get("Atlassian-Plugin-Key")).thenReturn("bundle2");
        //noinspection unchecked
        when(event.getModule()).thenReturn((ModuleDescriptor)moduleDescriptor);
        when(moduleDescriptor.getPlugin()).thenReturn(plugin1);
        when(plugin1.getKey()).thenReturn("bundle1");
        when(moduleDescriptor.getConfiguration()).thenReturn(aoConfig);
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

        serviceFactory.onTenantArrived(null);

        verify(babyBear1).startActiveObjects(tenant1);
        verify(babyBear2).startActiveObjects(tenant1);
    }

    public void onHotRestart()
    {
        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);
        serviceFactory.aoDelegatesByBundle.put(bundle2, babyBear2);

        when(tenantContext.getCurrentTenant()).thenReturn(tenant1);

        serviceFactory.onHotRestart(null);

        verify(babyBear1).restartActiveObjects(tenant1);
        verify(babyBear2).restartActiveObjects(tenant1);
    }

    @Test
    public void onPluginModuleEnabledEventNoDelegate()
    {
        serviceFactory.onPluginModuleEnabledEvent(event);

        assertThat(serviceFactory.unattachedConfigByPluginKey, hasEntry("bundle1", aoConfig));
    }

    @Test
    public void onPluginModuleEnabledEventHasDelegate()
    {
        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);

        serviceFactory.onPluginModuleEnabledEvent(event);

        assertThat(serviceFactory.unattachedConfigByPluginKey.isEmpty(), is(true));

        verify(babyBear1).setAoConfiguration(aoConfig);
    }
}
