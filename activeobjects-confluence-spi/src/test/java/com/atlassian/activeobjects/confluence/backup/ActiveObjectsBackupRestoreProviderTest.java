package com.atlassian.activeobjects.confluence.backup;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.BackupProgressMonitor;
import com.atlassian.activeobjects.spi.RestoreProgressMonitor;

@RunWith(MockitoJUnitRunner.class)
public class ActiveObjectsBackupRestoreProviderTest
{
    @Mock private Backup aoBackup;
    private ActiveObjectsBackupRestoreProvider provider;

    @Before
    public void setUp() throws Exception
    {
        provider = new ActiveObjectsBackupRestoreProvider();
        provider.setBackup(aoBackup);
    }

    @Test
    public void testBackup() throws Exception
    {
        OutputStream os = mock(OutputStream.class);
     
        provider.backup(os);
        
        verify(aoBackup).save(eq(os), any(BackupProgressMonitor.class));
    }

    @Test
    public void testRestore() throws Exception
    {
        InputStream is = mock(InputStream.class);
        
        provider.restore(is);
        
        verify(aoBackup).restore(eq(is), any(RestoreProgressMonitor.class));
    }
}
