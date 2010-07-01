package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.backup.Backup;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ActiveObjectsBackupFactoryImpl implements ActiveObjectsBackupFactory
{
    private static final Logger log = LoggerFactory.getLogger(ActiveObjectsBackupFactoryImpl.class);

    public Backup getBackup(final Bundle bundle, ActiveObjects ao)
    {
        return new AoBackup(bundle);
    }

    private static class AoBackup implements Backup
    {
        private final Bundle bundle;

        public AoBackup(Bundle bundle)
        {
            this.bundle = bundle;
        }

        public String getId()
        {
            return "ao_" + bundle.getSymbolicName() + "_" + bundle.getVersion();
        }

        public InputStream save()
        {
            return new ByteArrayInputStream(("Stream:" + getId()).getBytes());
        }

        public boolean accept(String id)
        {
            return true;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void restore(InputStream stream)
        {
            try
            {
                final String restored = IOUtils.toString(stream);
                log.info("Restored :\n" + restored);
            }
            catch (IOException e)
            {
                log.error("Error reading restore input stream!");
            }
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

            AoBackup aoBackup = (AoBackup) o;

            return getId().equals(aoBackup.getId());
        }
    }
}
