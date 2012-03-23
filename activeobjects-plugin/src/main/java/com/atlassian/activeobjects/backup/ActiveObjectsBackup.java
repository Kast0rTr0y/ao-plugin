package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.ao.PrefixedSchemaConfiguration;
import com.atlassian.activeobjects.internal.DatabaseProviderFactory;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.SimplePrefix;
import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.BackupProgressMonitor;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.RestoreProgressMonitor;
import com.atlassian.dbexporter.BatchMode;
import com.atlassian.dbexporter.CleanupMode;
import com.atlassian.dbexporter.ConnectionProvider;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.DbExporter;
import com.atlassian.dbexporter.DbImporter;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ImportExportConfiguration;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.activeobjects.spi.ImportExportException;
import com.atlassian.dbexporter.exporter.ConnectionProviderInformationReader;
import com.atlassian.dbexporter.exporter.DataExporter;
import com.atlassian.dbexporter.exporter.DatabaseInformationExporter;
import com.atlassian.dbexporter.exporter.ExportConfiguration;
import com.atlassian.dbexporter.exporter.TableDefinitionExporter;
import com.atlassian.dbexporter.importer.DataImporter;
import com.atlassian.dbexporter.importer.DatabaseInformationImporter;
import com.atlassian.dbexporter.importer.ImportConfiguration;
import com.atlassian.dbexporter.importer.SqlServerAroundTableImporter;
import com.atlassian.dbexporter.importer.TableDefinitionImporter;
import com.atlassian.dbexporter.node.NodeStreamReader;
import com.atlassian.dbexporter.node.NodeStreamWriter;
import com.atlassian.dbexporter.node.stax.StaxStreamReader;
import com.atlassian.dbexporter.node.stax.StaxStreamWriter;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import net.java.ao.DatabaseProvider;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.NameConverters;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import static com.atlassian.activeobjects.ao.ConverterUtils.*;
import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsBackup implements Backup
{
    public static final Prefix PREFIX = new SimplePrefix("AO");

    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final String NAMESPACE = "http://www.atlassian.com/ao";

    private final Supplier<DatabaseProvider> databaseProviderSupplier;
    private final NameConverters nameConverters;
    private final ImportExportErrorService errorService;

    public ActiveObjectsBackup(final DatabaseProviderFactory databaseProviderFactory, final DataSourceProvider dataSourceProvider, NameConverters converters, ImportExportErrorService errorService)
    {
        this(new Supplier<DatabaseProvider>()
        {
            @Override
            public DatabaseProvider get()
            {
                return checkNotNull(databaseProviderFactory).getDatabaseProvider(dataSourceProvider.getDataSource(), dataSourceProvider.getDatabaseType(), dataSourceProvider.getSchema());
            }
        }, converters, errorService);
    }

    ActiveObjectsBackup(DatabaseProvider databaseProvider, NameConverters converters, ImportExportErrorService errorService)
    {
        this(Suppliers.ofInstance(checkNotNull(databaseProvider)), converters, errorService);
    }

    private ActiveObjectsBackup(Supplier<DatabaseProvider> databaseProviderSupplier, NameConverters converters, ImportExportErrorService errorService)
    {
        this.databaseProviderSupplier = checkNotNull(databaseProviderSupplier);
        this.nameConverters = checkNotNull(converters);
        this.errorService = checkNotNull(errorService);
    }

    /**
     * Saves the backup to an output stream.
     *
     * @param stream the stream to write the backup to
     * @param monitor the progress monitor for the current backup
     * @throws ImportExportException or one of its sub-types if any error happens during the backup.
     * {@link java.sql.SQLException SQL exceptions} will be wrapped in {@link ImportExportException}.
     */
    public void save(OutputStream stream, BackupProgressMonitor monitor)
    {
        final DatabaseProvider provider = databaseProviderSupplier.get();
        final DatabaseProviderConnectionProvider connectionProvider = getConnectionProvider(provider);
        final ExportConfiguration configuration = new ActiveObjectsExportConfiguration(connectionProvider, getProgressMonitor(monitor));

        final DbExporter dbExporter = new DbExporter(
                new DatabaseInformationExporter(new ConnectionProviderInformationReader(errorService, connectionProvider)),
                new TableDefinitionExporter(new ActiveObjectsTableReader(errorService, nameConverters, provider, schemaConfiguration())),
                new DataExporter(errorService, provider.getSchema()));


        NodeStreamWriter streamWriter = null;
        try
        {
            streamWriter = new StaxStreamWriter(errorService, new OutputStreamWriter(stream, CHARSET), CHARSET, NAMESPACE);
            dbExporter.exportData(streamWriter, configuration);
            streamWriter.flush();
        }
        finally
        {
            closeCloseable(streamWriter);
        }
    }

    public static SchemaConfiguration schemaConfiguration()
    {
        return new PrefixedSchemaConfiguration(PREFIX);
    }

    /**
     * Restores the backup coming from the given input stream.
     *
     * @param stream the stream of data previously backed up by the plugin.
     * @param monitor the progress monitor for the current restore
     * @throws ImportExportException or one of its sub-types if any error happens during the backup.
     * {@link java.sql.SQLException SQL exceptions} will be wrapped in {@link ImportExportException}.
     */
    public void restore(InputStream stream, RestoreProgressMonitor monitor)
    {
        final DatabaseProvider provider = databaseProviderSupplier.get();
        final DatabaseProviderConnectionProvider connectionProvider = getConnectionProvider(provider);

        final DatabaseInformation databaseInformation = getDatabaseInformation(connectionProvider);

        final ImportConfiguration configuration = new ActiveObjectsImportConfiguration(connectionProvider, getProgressMonitor(monitor), databaseInformation);

        final DbImporter dbImporter = new DbImporter(errorService,
                new DatabaseInformationImporter(errorService),
                new TableDefinitionImporter(errorService, new ActiveObjectsTableCreator(errorService, provider, nameConverters), new ActiveObjectsDatabaseCleaner(provider, nameConverters, schemaConfiguration(), errorService)),
                new DataImporter(errorService,
                        provider.getSchema(),
                        new SqlServerAroundTableImporter(errorService, provider.getSchema()),
                        new PostgresSequencesAroundImporter(errorService, provider),
                        new OracleSequencesAroundImporter(errorService, provider, nameConverters),
                        new ForeignKeyAroundImporter(new ActiveObjectsForeignKeyCreator(errorService, nameConverters, provider))
                ));

        NodeStreamReader streamReader = null;
        try
        {
            streamReader = new StaxStreamReader(errorService, new InputStreamReader(stream, CHARSET));
            dbImporter.importData(streamReader, configuration);
        }
        finally
        {
            closeCloseable(streamReader);
        }
    }

    @Override
    public void clear()
    {
        final DatabaseProvider provider = databaseProviderSupplier.get();
        new ActiveObjectsDatabaseCleaner(provider, nameConverters, schemaConfiguration(), errorService).cleanup(CleanupMode.CLEAN);
    }

    private DatabaseInformation getDatabaseInformation(DatabaseProviderConnectionProvider connectionProvider)
    {
        return new DatabaseInformation(new ConnectionProviderInformationReader(errorService, connectionProvider).get());
    }

    private static DatabaseProviderConnectionProvider getConnectionProvider(DatabaseProvider provider)
    {
        return new DatabaseProviderConnectionProvider(provider);
    }

    private ProgressMonitor getProgressMonitor(BackupProgressMonitor backupProgressMonitor)
    {
        return new ActiveObjectsBackupProgressMonitor(backupProgressMonitor);
    }

    private ProgressMonitor getProgressMonitor(RestoreProgressMonitor restoreProgressMonitor)
    {
        return new ActiveObjectsRestoreProgressMonitor(restoreProgressMonitor);
    }

    private static void closeCloseable(Closeable streamWriter)
    {
        if (streamWriter != null)
        {
            try
            {
                streamWriter.close();
            }
            catch (IOException e)
            {
                // ignore
            }
        }
    }

    private static abstract class ActiveObjectsImportExportConfiguration implements ImportExportConfiguration
    {
        private final ConnectionProvider connectionProvider;
        private final ProgressMonitor progressMonitor;
        private final EntityNameProcessor entityNameProcessor;

        ActiveObjectsImportExportConfiguration(ConnectionProvider connectionProvider, ProgressMonitor progressMonitor)
        {
            this.connectionProvider = checkNotNull(connectionProvider);
            this.progressMonitor = checkNotNull(progressMonitor);
            this.entityNameProcessor = new UpperCaseEntityNameProcessor();
        }

        @Override
        public final ConnectionProvider getConnectionProvider()
        {
            return connectionProvider;
        }

        @Override
        public final ProgressMonitor getProgressMonitor()
        {
            return progressMonitor;
        }

        @Override
        public final EntityNameProcessor getEntityNameProcessor()
        {
            return entityNameProcessor;
        }
    }

    private static final class ActiveObjectsExportConfiguration extends ActiveObjectsImportExportConfiguration implements ExportConfiguration
    {
        public ActiveObjectsExportConfiguration(ConnectionProvider connectionProvider, ProgressMonitor progressMonitor)
        {
            super(connectionProvider, progressMonitor);
        }
    }

    private static final class ActiveObjectsImportConfiguration extends ActiveObjectsImportExportConfiguration implements ImportConfiguration
    {
        private final DatabaseInformation databaseInformation;

        ActiveObjectsImportConfiguration(ConnectionProvider connectionProvider, ProgressMonitor progressMonitor, DatabaseInformation databaseInformation)
        {
            super(connectionProvider, progressMonitor);
            this.databaseInformation = checkNotNull(databaseInformation);
        }

        @Override
        public DatabaseInformation getDatabaseInformation()
        {
            return databaseInformation;
        }

        @Override
        public CleanupMode getCleanupMode()
        {
            return CleanupMode.CLEAN;
        }

        @Override
        public BatchMode getBatchMode()
        {
            return BatchMode.ON;
        }
    }

    public static final class UpperCaseEntityNameProcessor implements EntityNameProcessor
    {
        @Override
        public String tableName(String tableName)
        {
            return toUpperCase(tableName);
        }

        @Override
        public String columnName(String columnName)
        {
            return toUpperCase(columnName);
        }
    }
}
