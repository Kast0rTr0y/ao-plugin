package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.internal.DatabaseProviderFactory;
import com.atlassian.activeobjects.internal.SimplePrefix;
import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.BackupError;
import com.atlassian.activeobjects.spi.BackupProgressMonitor;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.BackupErrorListener;
import com.atlassian.activeobjects.spi.OnBackupError;
import com.atlassian.activeobjects.spi.PluginExport;
import com.atlassian.activeobjects.spi.PluginImport;
import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.activeobjects.spi.RestoreProgressMonitor;
import com.atlassian.dbexporter.ImportExportException;
import com.google.common.base.Supplier;
import net.java.ao.DatabaseProvider;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static com.atlassian.activeobjects.plugin.ActiveObjectModuleDescriptor.*;
import static com.atlassian.activeobjects.spi.OnBackupError.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

public final class BackupImpl implements Backup
{
    private final Supplier<DatabaseProvider> databaseProviderSupplier;
    private final PrefixedSchemaConfigurationFactory schemaConfigurationFactory;
    private final ActiveObjectsHashesReader hashesReader;
    private final ActiveObjectsBackupFactory backupFactory;
    private final PluginInformationFactory pluginInformationFactory;

    public BackupImpl(DatabaseProviderFactory databaseProviderFactory,
                      DataSourceProvider dataSourceProvider,
                      PrefixedSchemaConfigurationFactory schemaConfigurationFactory,
                      ActiveObjectsHashesReader hashesReader,
                      ActiveObjectsBackupFactory backupFactory,
                      PluginInformationFactory pluginInformationFactory)
    {
        this(
                newDatabaseProviderSupplier(databaseProviderFactory, dataSourceProvider),
                schemaConfigurationFactory,
                hashesReader,
                backupFactory,
                pluginInformationFactory);
    }

    private BackupImpl(Supplier<DatabaseProvider> databaseProvider,
                       PrefixedSchemaConfigurationFactory schemaConfigurationFactory,
                       ActiveObjectsHashesReader hashesReader,
                       ActiveObjectsBackupFactory backupFactory,
                       PluginInformationFactory pluginInformationFactory)
    {
        this.databaseProviderSupplier = checkNotNull(databaseProvider);
        this.schemaConfigurationFactory = checkNotNull(schemaConfigurationFactory);
        this.hashesReader = checkNotNull(hashesReader);
        this.backupFactory = checkNotNull(backupFactory);
        this.pluginInformationFactory = checkNotNull(pluginInformationFactory);
    }

    @Override
    public List<BackupError> save(PluginExport export, BackupProgressMonitor monitor)
    {
        final DatabaseProvider databaseProvider = databaseProviderSupplier.get();

        final List<BackupError> errors = newArrayList();

        for (String hash : hashesReader.getHashes(databaseProvider, schemaConfigurationFactory))
        {
            final ActiveObjectsBackup activeObjectsBackup = newAoBackup(databaseProvider, hash);
            final PluginInformation info = pluginInformationFactory.getPluginInformation(hash);

            OutputStream os = null;
            try
            {
                os = export.getOutputStream(info);
                activeObjectsBackup.save(os, monitor, info);
            }
            catch (ImportExportException e)
            {
                handleError(errors, export, info, e);
            }
            finally
            {
                closeQuietly(os);
            }
        }
        return errors;
    }

    @Override
    public List<BackupError> restore(Iterable<PluginImport> imports, RestoreProgressMonitor monitor)
    {
        final DatabaseProvider databaseProvider = databaseProviderSupplier.get();
        final List<BackupError> errors = newArrayList();

        for (PluginImport imp : imports)
        {
            final PluginInformation info = getPluginInformation(imp);
            final ActiveObjectsBackup activeObjectsBackup = newAoBackup(databaseProvider, info.getHash());

            final PluginInformationChecker pluginInformationChecker = new PluginInformationChecker();

            InputStream is = null;
            try
            {
                is = imp.getInputStream();
                activeObjectsBackup.restore(is, monitor, pluginInformationChecker);
            }
            catch (ImportExportException e)
            {
                handleError(errors, imp, pluginInformationChecker.getPluginInformation().isAvailable() ? pluginInformationChecker.getPluginInformation() : info, e);
            }
            finally
            {
                closeQuietly(is);
            }
        }
        return errors;
    }

    private void handleError(List<BackupError> errors, BackupErrorListener backupErrorListener, final PluginInformation info, final ImportExportException e)
    {
        final OnBackupError error = backupErrorListener.error(info, e);
        if (SKIP.equals(error))
        {
            errors.add(new BackupErrorImpl(info, e));
        }
        else
        {
            throw e;
        }
    }

    private PluginInformation getPluginInformation(PluginImport pluginImport)
    {
        final PluginInformation pluginInformation = pluginImport.getPluginInformation();
        if (!pluginInformation.isAvailable())
        {
            // try to find out more
            return pluginInformationFactory.getPluginInformation(pluginInformation.getHash());
        }
        else
        {
            return pluginInformation;
        }
    }

    private ActiveObjectsBackup newAoBackup(DatabaseProvider databaseProvider, String hash)
    {
        return backupFactory.newAoBackup(databaseProvider, schemaConfigurationFactory, newPrefix(hash));
    }

    private static SimplePrefix newPrefix(String hash)
    {
        return new SimplePrefix(AO_TABLE_PREFIX + "_" + hash);
    }

    private static Supplier<DatabaseProvider> newDatabaseProviderSupplier(final DatabaseProviderFactory databaseProviderFactory, final DataSourceProvider dataSourceProvider)
    {
        return new Supplier<DatabaseProvider>()
        {
            @Override
            public DatabaseProvider get()
            {
                return checkNotNull(databaseProviderFactory).getDatabaseProvider(dataSourceProvider.getDataSource(), dataSourceProvider.getDatabaseType());
            }
        };
    }

    private static class BackupErrorImpl implements BackupError
    {
        private final PluginInformation info;
        private final ImportExportException e;

        public BackupErrorImpl(PluginInformation info, ImportExportException e)
        {
            this.info = info;
            this.e = e;
        }

        @Override
        public PluginInformation getPluginInformation()
        {
            return info;
        }

        @Override
        public Throwable getThrowable()
        {
            return e;
        }
    }

    private static void closeQuietly(Closeable closeable)
    {
        try
        {
            if (closeable != null)
            {
                closeable.close();
            }
        }
        catch (IOException ioe)
        {
            // ignore
        }
    }
}
