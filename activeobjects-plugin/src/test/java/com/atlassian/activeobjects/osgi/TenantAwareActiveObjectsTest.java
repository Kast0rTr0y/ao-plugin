package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.NoDataSourceException;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.spi.TenantProvider;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;
import com.google.common.base.Function;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith (MockitoJUnitRunner.class)
public class TenantAwareActiveObjectsTest
{
    private TenantAwareActiveObjects babyBear;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private Bundle bundle;
    @Mock
    private ActiveObjectsFactory factory;
    @Mock
    private TenantProvider tenantProvider;
    @Mock
    private AOConfigurationGenerator aoConfigurationGenerator;
    @Mock
    private Function<Tenant, ExecutorService> initExecutorFunction;
    @Mock
    private ScheduledExecutorService configExecutor;
    @Mock
    private PluginAccessor pluginAccessor;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Tenant tenant;

    @Mock
    private ServiceEvent serviceEvent;

    @Mock
    private ServiceReference serviceReference;

    @Mock
    private ActiveObjects ao;

    @Mock
    private ActiveObjectsConfiguration aoConfig;

    @Before
    public void before()
    {
        babyBear = new TenantAwareActiveObjects(bundle, factory, tenantProvider, aoConfigurationGenerator, initExecutorFunction, pluginAccessor);

        when(bundle.getSymbolicName()).thenReturn("some.bundle");
        when(bundle.getBundleContext()).thenReturn(bundleContext);

        when(bundleContext.getService(serviceReference)).thenReturn(aoConfig);

        when(serviceEvent.getServiceReference()).thenReturn(serviceReference);
    }

    @Test
    public void delegateTenanted() throws ExecutionException, InterruptedException
    {
        when(tenantProvider.getCurrentTenant()).thenReturn(tenant);

        final Promise<ActiveObjects> aoPromise = Promises.promise(ao);

        babyBear.aoPromisesByTenant.put(tenant, aoPromise);

        assertSame(babyBear.delegate().get(), ao);
    }

    @Test
    public void delgateUntenanted()
    {
        expectedException.expect(NoDataSourceException.class);

        babyBear.delegate();
    }

    @Test
    public void awaitInitNoTenant() throws ExecutionException, InterruptedException
    {
        expectedException.expect(NoDataSourceException.class);

        babyBear.moduleMetaData().awaitInitialization();
    }

    @Test
    public void awaitInitTimedNoTenant() throws ExecutionException, InterruptedException
    {
        expectedException.expect(NoDataSourceException.class);

        babyBear.moduleMetaData().awaitInitialization();
    }

    @Test
    public void isInitializedNoTenant()
    {
        assertThat(babyBear.moduleMetaData().isInitialized(), is(false));
    }

    @Test
    public void isInitializedNotComplete()
    {
        when(tenantProvider.getCurrentTenant()).thenReturn(tenant);

        assertThat(babyBear.moduleMetaData().isInitialized(), is(false));
    }

    @Test
    public void isInitializedException()
    {
        when(tenantProvider.getCurrentTenant()).thenReturn(tenant);

        final SettableFuture<ActiveObjects> aoFuture = SettableFuture.create();
        aoFuture.setException(new IllegalStateException());
        final Promise<ActiveObjects> aoPromise = Promises.forFuture(aoFuture);
        babyBear.aoPromisesByTenant.put(tenant, aoPromise);

        assertThat(babyBear.moduleMetaData().isInitialized(), is(false));
    }

    @Test
    public void isInitializedComplete()
    {
        final Promise<ActiveObjects> aoPromise = Promises.promise(ao);
        babyBear.aoPromisesByTenant.put(tenant, aoPromise);

        when(tenantProvider.getCurrentTenant()).thenReturn(tenant);

        assertThat(babyBear.moduleMetaData().isInitialized(), is(true));
    }

    @Test
    public void isDataSourcePresentNo()
    {
        assertThat(babyBear.moduleMetaData().isDataSourcePresent(), is(false));
    }

    @Test
    public void isDataSourcePresentYes()
    {
        when(tenantProvider.getCurrentTenant()).thenReturn(tenant);

        assertThat(babyBear.moduleMetaData().isDataSourcePresent(), is(true));
    }
}
