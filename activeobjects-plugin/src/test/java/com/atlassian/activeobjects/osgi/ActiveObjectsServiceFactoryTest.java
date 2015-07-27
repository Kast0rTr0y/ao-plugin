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
import com.atlassian.plugin.impl.AbstractDelegatingPlugin;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
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

import java.util.Collections;
import java.util.Dictionary;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.never;
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
    private Tenant tenant3;
    @Mock
    private ExecutorService executorService1;
    @Mock
    private ExecutorService executorService2;
    @Mock
    private ExecutorService executorService3;
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
    private PluginModuleEnabledEvent moduleEnabledEvent;
    @Mock
    private ActiveObjectModuleDescriptor moduleDescriptor;
    @Mock
    private OsgiPlugin plugin1;
    @Mock
    private Plugin nonOsgiPlugin;
    @Mock
    private ActiveObjectsConfiguration aoConfig;

    @Before
    public void setUp() throws Exception
    {
        serviceFactory = new ActiveObjectsServiceFactory(factory, eventPublisher, tenantContext,
                threadLocalDelegateExecutorFactory, initExecutorServiceProvider);

        assertThat(serviceFactory.aoContextThreadFactory, instanceOf(ContextClassLoaderThreadFactory.class));
        assertThat(((ContextClassLoaderThreadFactory) serviceFactory.aoContextThreadFactory).getContextClassLoader(), sameInstance(Thread.currentThread().getContextClassLoader()));
        assertThat(serviceFactory.destroying, is(false));
        assertThat(serviceFactory.cleaning, is(false));

        when(babyBear1.getBundle()).thenReturn(bundle1);
        when(babyBear2.getBundle()).thenReturn(bundle2);
        when(bundle1.getHeaders()).thenReturn(bundle1Dictionary);
        when(bundle2.getHeaders()).thenReturn(bundle2Dictionary);
        //noinspection unchecked
        when(moduleEnabledEvent.getModule()).thenReturn((ModuleDescriptor) moduleDescriptor);
        when(moduleDescriptor.getPlugin()).thenReturn(plugin1);
        when(plugin1.getBundle()).thenReturn(bundle1);
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
        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);
        serviceFactory.aoDelegatesByBundle.put(bundle2, babyBear2);

        serviceFactory.destroy();

        assertThat(serviceFactory.destroying, is(true));

        verify(eventPublisher).unregister(serviceFactory);
        verify(babyBear1).destroy();
        verify(babyBear2).destroy();
        verify(executorService1).shutdownNow();
        verify(executorService2).shutdownNow();
    }

    @Test
    public void startCleaning() throws InterruptedException
    {
        serviceFactory.initExecutorsByTenant.put(tenant1, executorService1);
        serviceFactory.initExecutorsByTenant.put(tenant2, executorService2);
        serviceFactory.initExecutorsByTenant.put(tenant3, executorService3);

        when(executorService1.awaitTermination(ActiveObjectsServiceFactory.INIT_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException());
        when(executorService2.awaitTermination(ActiveObjectsServiceFactory.INIT_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS)).thenReturn(false);
        when(executorService3.awaitTermination(ActiveObjectsServiceFactory.INIT_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS)).thenReturn(true);

        serviceFactory.startCleaning();

        assertThat(serviceFactory.cleaning, is(true));

        verify(executorService1).shutdownNow();
        verify(executorService2).shutdownNow();
        verify(executorService3).shutdownNow();
        verify(executorService1).awaitTermination(ActiveObjectsServiceFactory.INIT_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        verify(executorService2).awaitTermination(ActiveObjectsServiceFactory.INIT_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        verify(executorService3).awaitTermination(ActiveObjectsServiceFactory.INIT_TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
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

        serviceFactory.ungetService(bundle1, null, babyBear1);

        assertThat(serviceFactory.aoDelegatesByBundle.asMap(), hasEntry(bundle2, babyBear2));
        assertThat(serviceFactory.aoDelegatesByBundle.asMap().size(), is(1));

        verify(babyBear1).destroy();
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

    @Test
    public void onHotRestart()
    {
        serviceFactory.initExecutorsByTenant.put(tenant1, executorService1);

        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);
        serviceFactory.aoDelegatesByBundle.put(bundle2, babyBear2);

        when(tenantContext.getCurrentTenant()).thenReturn(tenant1);

        serviceFactory.onHotRestart(null);

        verify(babyBear1).restartActiveObjects(tenant1);
        verify(babyBear2).restartActiveObjects(tenant1);
        verify(executorService1).shutdownNow();

        assertThat(serviceFactory.initExecutorsByTenant.asMap(), is(Collections.<Tenant, ExecutorService>emptyMap()));
    }

    @Test
    public void onPluginModuleEnabledEventNoDelegate()
    {
        serviceFactory.onPluginModuleEnabledEvent(moduleEnabledEvent);

        assertThat(serviceFactory.unattachedConfigByBundle, hasEntry(bundle1, aoConfig));
    }

    @Test
    public void onPluginModuleEnabledEventNonOsgi()
    {
        when(moduleDescriptor.getPlugin()).thenReturn(nonOsgiPlugin);

        assertThat(serviceFactory.unattachedConfigByBundle, is(Collections.<Bundle, ActiveObjectsConfiguration>emptyMap()));
    }

    @Test
    public void onPluginModuleEnabledEventHasDelegate()
    {
        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);

        serviceFactory.onPluginModuleEnabledEvent(moduleEnabledEvent);

        assertThat(serviceFactory.unattachedConfigByBundle, is(Collections.<Bundle, ActiveObjectsConfiguration>emptyMap()));

        verify(babyBear1).setAoConfiguration(aoConfig);
    }

    @Test
    public void aoDelegatesByBundleLoader() throws ExecutionException, InterruptedException
    {
        serviceFactory.unattachedConfigByBundle.put(bundle1, aoConfig);

        final TenantAwareActiveObjects aoDelegate = serviceFactory.aoDelegatesByBundle.get(bundle1);

        assertThat(aoDelegate, notNullValue());
        assertThat(aoDelegate.aoConfigFuture.isDone(), is(true));
        assertThat(aoDelegate.aoConfigFuture.get(), is(aoConfig));
    }

    @Test
    public void onPluginModuleEnabledEventHasDelegateForWrappedOsgiPlugin()
    {
        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);
        Plugin wrappedPlugin = new AbstractDelegatingPlugin(plugin1){};
        when(moduleDescriptor.getPlugin()).thenReturn(wrappedPlugin);
        serviceFactory.onPluginModuleEnabledEvent(moduleEnabledEvent);

        assertThat(serviceFactory.unattachedConfigByBundle, is(Collections.<Bundle, ActiveObjectsConfiguration>emptyMap()));

        verify(babyBear1).setAoConfiguration(aoConfig);
    }

    @Test
    public void onPluginModuleEnabledEventHasNoDelegateForWrappedPlugin()
    {
        serviceFactory.aoDelegatesByBundle.put(bundle1, babyBear1);
        Plugin wrappedPlugin = new AbstractDelegatingPlugin(nonOsgiPlugin)
        {
        };
        when(moduleDescriptor.getPlugin()).thenReturn(wrappedPlugin);
        serviceFactory.onPluginModuleEnabledEvent(moduleEnabledEvent);

        assertThat(serviceFactory.unattachedConfigByBundle, is(Collections.<Bundle, ActiveObjectsConfiguration>emptyMap()));
        verify(babyBear1, never()).setAoConfiguration(aoConfig);
    }
}
