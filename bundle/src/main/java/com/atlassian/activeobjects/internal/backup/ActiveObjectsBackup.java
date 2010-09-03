package com.atlassian.activeobjects.internal.backup;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.backup.Backup;
import org.osgi.framework.Bundle;

import java.io.InputStream;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

class ActiveObjectsBackup implements Backup
{
    private final Bundle bundle;
    private final ActiveObjects ao;

    public ActiveObjectsBackup(Bundle bundle, ActiveObjects ao)
    {
        this.bundle = checkNotNull(bundle);
        this.ao = checkNotNull(ao);
    }

    public String getId()
    {
        return "ao_" + bundle.getSymbolicName() + "_" + bundle.getVersion();
    }

    public InputStream save()
    {
        return ao.backup();
    }

    public boolean accept(String id)
    {
        return id.startsWith("ao_" + bundle.getSymbolicName() + "_");
    }

    public void restore(InputStream stream)
    {
        ao.restore(stream);
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
