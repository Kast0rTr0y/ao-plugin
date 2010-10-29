package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.internal.DatabaseProviderFactory;
import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import net.java.ao.DatabaseProvider;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.BackupRestore;
import net.java.ao.schema.ddl.DDLAction;
import org.osgi.framework.Bundle;

import java.io.InputStream;
import java.io.OutputStream;

import static com.atlassian.activeobjects.util.ActiveObjectsUtils.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;

public final class ActiveObjectsBackup implements Backup
{
    private final BackupId backupId;
    private final DatabaseProviderFactory databaseProviderFactory;
    private final DataSourceProvider dataSourceProvider;
    private final SchemaConfiguration schemaConfiguration;
    private final BackupRestore backupRestore;
    private final BackupSerialiser<Iterable<DDLAction>> serialiser;

    public ActiveObjectsBackup(Bundle bundle, DatabaseProviderFactory databaseProviderFactory, DataSourceProvider dataSourceProvider, SchemaConfiguration schemaConfiguration, BackupSerialiser<Iterable<DDLAction>> serialiser, BackupRestore backupRestore)
    {
        this.backupId = BackupId.fromBundle(checkNotNull(bundle));
        this.databaseProviderFactory = checkNotNull(databaseProviderFactory);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
        this.schemaConfiguration = checkNotNull(schemaConfiguration);
        this.serialiser = checkNotNull(serialiser);
        this.backupRestore = checkNotNull(backupRestore);
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
        final DatabaseProvider provider = getProvider();
        try
        {
            serialiser.serialise(backupRestore.backup(provider, schemaConfiguration), os);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            provider.dispose();
        }
    }

    public void restore(String id, InputStream backup)
    {
        if (accept(id))
        {
            final DatabaseProvider provider = getProvider();
            try
            {
                backupRestore.restore(newLinkedList(serialiser.deserialise(backup)), provider);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            finally
            {
                provider.dispose();
            }
        }
    }

    private DatabaseProvider getProvider()
    {
        return databaseProviderFactory.getDatabaseProvider(dataSourceProvider.getDataSource(), dataSourceProvider.getDatabaseType());
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
