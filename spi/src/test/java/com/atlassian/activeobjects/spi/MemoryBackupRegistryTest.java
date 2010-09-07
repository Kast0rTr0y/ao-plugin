package com.atlassian.activeobjects.spi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testing {@link com.atlassian.activeobjects.spi.MemoryBackupRegistry}
 */
@RunWith(MockitoJUnitRunner.class)
public class MemoryBackupRegistryTest
{
    private MemoryBackupRegistry backupRegistry;

    private Set<Backup> backups;

    @Mock
    private Backup backup;

    @Before
    public void setUp()
    {
        backups = new HashSet<Backup>();
        backupRegistry = new MemoryBackupRegistry(backups);
    }

    @After
    public void tearDown()
    {
        backupRegistry = null;
        backups = null;
    }

    @Test
    public void testRegister() throws Exception
    {
        backupRegistry.register(backup);
        assertFalse(backups.isEmpty());
        assertEquals(backup, backups.iterator().next());
    }

    @Test
    public void testUnregister() throws Exception
    {
        assertTrue(backups.isEmpty());

        backupRegistry.unregister(backup);
        assertTrue(backups.isEmpty());

        backupRegistry.register(backup);
        assertFalse(backups.isEmpty());
        backupRegistry.unregister(backup);
        assertTrue(backups.isEmpty());
    }
}
