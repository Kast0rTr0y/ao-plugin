package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.spi.Backup;
import org.osgi.framework.Bundle;

import java.io.InputStream;
import java.io.OutputStream;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;

class ActiveObjectsBackup implements Backup
{
    private final BackupId backupId;

    public ActiveObjectsBackup(Bundle bundle)
    {
        this.backupId = BackupId.fromBundle(checkNotNull(bundle));
    }

    public String getId()
    {
        return backupId.toString();
    }

    public boolean accept(String id)
    {
        return accept(BackupId.fromString(id));
    }

    private boolean accept(BackupId backupId)
    {
        return this.backupId.isCompatible(backupId);
    }

    public void save(OutputStream os)
    {
        unsupported();
    }

    public void restore(String id, InputStream stream)
    {
        if (accept(id))
        {
            unsupported();
        }
        else
        {
            throw new IllegalStateException("Cannot restore backup with ID " + id + ".");
        }
    }

    private void unsupported()
    {
        throw new UnsupportedOperationException("Backup is not yet supported as part of the Active Objects plugin");
    }

    @Override
    public int hashCode()
    {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActiveObjectsBackup aoBackup = (ActiveObjectsBackup) o;

        return getId().equals(aoBackup.getId());
    }
}
