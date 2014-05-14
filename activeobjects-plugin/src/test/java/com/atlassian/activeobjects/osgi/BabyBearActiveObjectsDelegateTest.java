package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.NoDataSourceException;
import com.atlassian.activeobjects.internal.ActiveObjectsFactory;
import com.atlassian.activeobjects.internal.TenantProvider;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.tenancy.api.Tenant;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;
import com.google.common.base.Function;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith (MockitoJUnitRunner.class)
public class BabyBearActiveObjectsDelegateTest
{
    private BabyBearActiveObjectsDelegate babyBear;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private Bundle bundle;
    @Mock
    private ActiveObjectsFactory factory;
    @Mock
    private DataSourceProvider dataSourceProvider;
    @Mock
    private TenantProvider tenantProvider;
    @Mock
    private AOConfigurationGenerator aoConfigurationGenerator;
    @Mock
    private Function<Tenant, ExecutorService> initExecutorFunction;
    @Mock
    private ScheduledExecutorService configExecutor;

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

    @BeforeClass
    public static void beforeClass()
    {
        // don't forget to update maven-surefire-plugin confugration
        System.setProperty(BabyBearActiveObjectsDelegate.CONFIGURATION_TIMEOUT_MS_PROPERTY, String.valueOf(999));
    }

    @Before
    public void before()
    {
        babyBear = new BabyBearActiveObjectsDelegate(bundle, factory, dataSourceProvider, tenantProvider,
                aoConfigurationGenerator, initExecutorFunction, configExecutor);

        when(bundle.getSymbolicName()).thenReturn("some.bundle");
        when(bundle.getBundleContext()).thenReturn(bundleContext);

        when(bundleContext.getService(serviceReference)).thenReturn(aoConfig);

        when(serviceEvent.getServiceReference()).thenReturn(serviceReference);
    }

    @Test
    public void init() throws InvalidSyntaxException
    {
        final String filter = "(&(objectclass=" + ActiveObjectsConfiguration.class.getName() + ")(com.atlassian.plugin.key=" + bundle.getSymbolicName() + "))";

        babyBear.init();

        verify(bundle, times(2)).getBundleContext();
        verify(bundleContext).addServiceListener(babyBear, filter);

        verify(configExecutor).schedule(babyBear.configCheckRunnable, 999, TimeUnit.MILLISECONDS);
    }

    @Test
    public void initOnePresent() throws InvalidSyntaxException, ExecutionException, InterruptedException
    {
        final String filter = "(&(objectclass=" + ActiveObjectsConfiguration.class.getName() + ")(com.atlassian.plugin.key=" + bundle.getSymbolicName() + "))";

        final ServiceReference[] serviceReferences = new ServiceReference[] { mock(ServiceReference.class) };

        when(bundleContext.getServiceReferences(ActiveObjectsConfiguration.class.getName(), filter)).thenReturn(serviceReferences);
        when(bundleContext.getService(serviceReferences[0])).thenReturn(aoConfig);

        babyBear.init();

        assertThat(babyBear.aoConfigFutureRef.get().isDone(), is(true));
        assertThat(babyBear.aoConfigFutureRef.get().get(), is(aoConfig));

        verify(bundle, times(3)).getBundleContext();
        verify(bundleContext).addServiceListener(babyBear, filter);
        verify(bundleContext).getServiceReferences(any(String.class), any(String.class));
        verify(bundleContext).getService(serviceReferences[0]);

        verify(configExecutor).schedule(babyBear.configCheckRunnable, 999, TimeUnit.MILLISECONDS);
    }

    @Test
    public void initManyPresent() throws InvalidSyntaxException
    {
        final String filter = "(&(objectclass=" + ActiveObjectsConfiguration.class.getName() + ")(com.atlassian.plugin.key=" + bundle.getSymbolicName() + "))";

        final ServiceReference[] serviceReferences = new ServiceReference[] { mock(ServiceReference.class), mock(ServiceReference.class) };

        when(bundleContext.getServiceReferences(ActiveObjectsConfiguration.class.getName(), filter)).thenReturn(serviceReferences);

        expectedException.expect(IllegalStateException.class);

        babyBear.init();
    }

    @Test
    public void serviceChangedRegisteredSingle() throws ExecutionException, InterruptedException
    {
        when(serviceEvent.getType()).thenReturn(ServiceEvent.REGISTERED);

        babyBear.serviceChanged(serviceEvent);

        assertTrue("AO configuration future is not fulfilled", babyBear.aoConfigFutureRef.get().isDone());
        assertSame(aoConfig, babyBear.aoConfigFutureRef.get().get());
    }

    @Test
    public void serviceChangedRegisteredMultiple()
    {
        final SettableFuture<ActiveObjectsConfiguration> existingAoConfigFutureRef = SettableFuture.create();
        existingAoConfigFutureRef.set(mock(ActiveObjectsConfiguration.class));
        babyBear.aoConfigFutureRef.set(existingAoConfigFutureRef);

        when(serviceEvent.getType()).thenReturn(ServiceEvent.REGISTERED);

        expectedException.expect(IllegalStateException.class);

        babyBear.serviceChanged(serviceEvent);
    }

    @Test
    public void serviceChangedUnregistering()
    {
        final SettableFuture<ActiveObjectsConfiguration> originalAoConfigFuture = SettableFuture.create();
        originalAoConfigFuture.set(aoConfig);
        babyBear.aoConfigFutureRef.set(originalAoConfigFuture);

        when(serviceEvent.getType()).thenReturn(ServiceEvent.UNREGISTERING);

        babyBear.serviceChanged(serviceEvent);

        verify(bundleContext).ungetService(serviceReference);
        assertTrue("AO configuration future is not fulfilled", babyBear.aoConfigFutureRef.get().isDone());
        assertNotSame(originalAoConfigFuture, babyBear.aoConfigFutureRef.get());

        try
        {
            babyBear.aoConfigFutureRef.get().get();
            fail("ExecutionException wrapping IllegalStateException not thrown");
        }
        catch (Exception e)
        {
            assertThat(e, is(ExecutionException.class));
            assertThat(e.getCause(), is(IllegalStateException.class));
        }
    }

    @Test
    public void delegateTenanted() throws ExecutionException, InterruptedException
    {
        when(tenantProvider.getTenant()).thenReturn(tenant);

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
    public void isInitialized()
    {
        when(tenantProvider.getTenant()).thenReturn(tenant);

        assertFalse("should not be initialised yet", babyBear.moduleMetaData().isInitialized());
    }
}
