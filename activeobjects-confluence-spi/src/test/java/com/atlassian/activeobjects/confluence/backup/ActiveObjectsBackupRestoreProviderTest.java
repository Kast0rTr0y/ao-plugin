package com.atlassian.activeobjects.confluence.backup;

import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.BackupProgressMonitor;
import com.atlassian.activeobjects.spi.HotRestartEvent;
import com.atlassian.activeobjects.spi.RestoreProgressMonitor;
import com.atlassian.activeobjects.spi.TransactionSynchronisationManager;
import com.atlassian.event.api.EventPublisher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.io.OutputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActiveObjectsBackupRestoreProviderTest {
    @Mock
    private Backup aoBackup;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private TransactionSynchronisationManager tranSyncManager;
    private ActiveObjectsBackupRestoreProvider provider;

    @Before
    public void setUp() throws Exception {
        provider = new ActiveObjectsBackupRestoreProvider();
        provider.setBackup(aoBackup);
        provider.setEventPublisher(eventPublisher);
        provider.setTransactionSynchManager(tranSyncManager);
        when(tranSyncManager.runOnSuccessfulCommit(any(Runnable.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return true;
            }
        });
    }

    @Test
    public void testBackup() throws Exception {
        OutputStream os = mock(OutputStream.class);

        provider.backup(os);

        verify(aoBackup).save(eq(os), any(BackupProgressMonitor.class));

    }

    @Test
    public void testRestore() throws Exception {
        InputStream is = mock(InputStream.class);

        provider.restore(is);

        verify(aoBackup).restore(eq(is), any(RestoreProgressMonitor.class));
        verify(eventPublisher).publish(HotRestartEvent.INSTANCE);
    }
}
