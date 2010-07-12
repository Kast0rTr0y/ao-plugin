package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.backup.Backup;
import net.java.ao.schema.SchemaGenerator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.impl.Log4JLogger;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

class ActiveObjectsBackup implements Backup
{
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Bundle bundle;
    private final ActiveObjects ao;

    public ActiveObjectsBackup(Bundle bundle, ActiveObjects ao)
    {
        this.bundle = bundle;
        this.ao = ao;
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
