package com.atlassian.activeobjects.spi;

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple implementation of the backup registry that keeps backups in memory.
 */
public class MemoryBackupRegistry implements BackupRegistry
{
    private final Set<Backup> backups;

    public MemoryBackupRegistry()
    {
        this(new CopyOnWriteArraySet<Backup>());
    }

    public MemoryBackupRegistry(Set<Backup> backups)
    {
        this.backups = checkNotNull(backups);
    }

    public final void register(Backup backup)
    {
        backups.add(backup);
    }

    public final void unregister(Backup backup)
    {
        backups.remove(backup);
    }

    public Set<Backup> getBackups()
    {
        return ImmutableSet.copyOf(backups);
    }
}
