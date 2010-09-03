package com.atlassian.activeobjects.internal.backup;

import com.atlassian.sal.api.backup.Backup;
import com.atlassian.sal.api.backup.BackupRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.service.ServiceUnavailableException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link com.atlassian.activeobjects.internal.backup.SpringAwareBackupRegistry}
 */
@RunWith(MockitoJUnitRunner.class)
public class SpringAwareBackupRegistryTest
{
    private SpringAwareBackupRegistry springAwareBackupRegistry;

    @Mock
    private BackupRegistry delegateBackupRegistry;

    @Mock
    private Backup backup;

    @Before
    public void setUp() throws Exception
    {
        springAwareBackupRegistry = new SpringAwareBackupRegistry(delegateBackupRegistry);
    }

    @Test
    public void testRegisterWithAvailableBackupRegistry() throws Exception
    {
        springAwareBackupRegistry.register(backup);
        verify(delegateBackupRegistry).register(backup);
    }

    @Test
    public void testRegisterWithUnAvailableBackupRegistry() throws Exception
    {
        backupRegistryUnavailable();
        springAwareBackupRegistry.register(backup);
    }

    @Test
    public void testUnregisterWithAvailableBackupRegistry() throws Exception
    {
        springAwareBackupRegistry.unregister(backup);
        verify(delegateBackupRegistry).unregister(backup);
    }

    @Test
    public void testUnregisterWithUnAvailableBackupRegistry() throws Exception
    {
        backupRegistryUnavailable();
        springAwareBackupRegistry.unregister(backup);
    }

    @Test
    public void testGetRegisteredWithAvailableBackupRegistry() throws Exception
    {
        springAwareBackupRegistry.getRegistered();
        verify(delegateBackupRegistry).getRegistered();
    }

    @Test
    public void testGetRegisteredWithUnAvailableBackupRegistry() throws Exception
    {
        backupRegistryUnavailable();
        assertTrue(springAwareBackupRegistry.getRegistered().isEmpty());
    }

    private void backupRegistryUnavailable()
    {
        final ServiceUnavailableException e = newServiceUnavailableException();
        doThrow(e).when(delegateBackupRegistry).register(anyBackup());
        doThrow(e).when(delegateBackupRegistry).unregister(anyBackup());
        doThrow(e).when(delegateBackupRegistry).getRegistered();
    }

    private static ServiceUnavailableException newServiceUnavailableException()
    {
        final ServiceReference reference = mock(ServiceReference.class);
        when(reference.getProperty(Constants.SERVICE_ID)).thenReturn(new Long(1L));
        return new ServiceUnavailableException(reference);
    }

    private static Backup anyBackup()
    {
        return Mockito.any();
    }
}
