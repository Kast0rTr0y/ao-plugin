package com.atlassian.activeobjects.osgi;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.internal.ActiveObjectsProvider;
import com.atlassian.activeobjects.internal.backup.ActiveObjectsBackupFactory;
import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.sal.api.backup.Backup;
import com.atlassian.sal.api.backup.BackupRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class ActiveObjectsServiceFactoryTest
{
    private ActiveObjectsServiceFactory serviceFactory;

    @Mock
    private ActiveObjectOsgiServiceUtils<ActiveObjectsConfiguration> osgiUtils;

    @Mock
    private ActiveObjectsConfiguration configuration;

    @Mock
    private ActiveObjectsProvider provider;

    @Mock
    private BackupRegistry backupRegistry;

    @Mock
    private ActiveObjectsBackupFactory backupFactory;

    @Mock
    private Bundle bundle;

    @Mock
    private Backup backup;

    @Before
    public void setUp() throws Exception
    {
        serviceFactory = new ActiveObjectsServiceFactory(osgiUtils, provider, backupRegistry, backupFactory);

        when(osgiUtils.getService(bundle)).thenReturn(configuration);
        when(backupFactory.getBackup(eq(bundle), anyActiveObjects())).thenReturn(backup);
    }

    @Test
    public void testGetService()
    {
        final Object ao = serviceFactory.getService(bundle, null); // the service registration is not used
        assertNotNull(ao);
        assertTrue(ao instanceof DelegatingActiveObjects);

        Assert.assertEquals(configuration, ((DelegatingActiveObjects) ao).getConfiguration());
        assertEquals(provider, ((DelegatingActiveObjects) ao).getProvider());

        verify(backupRegistry).register(backup);
    }

    @Test
    public void testUnGetService()
    {
        serviceFactory.ungetService(bundle, null, null);
        verifyZeroInteractions(provider);
        verify(backupRegistry).unregister(backup);
    }

    private static ActiveObjects anyActiveObjects()
    {
        return Mockito.any();
    }
}
