package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.spi.BackupProgressMonitor;
import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.activeobjects.spi.RestoreProgressMonitor;
import com.atlassian.dbexporter.BatchMode;
import com.atlassian.dbexporter.CleanupMode;
import com.atlassian.dbexporter.ConnectionProvider;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.DbExporter;
import com.atlassian.dbexporter.DbImporter;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ImportExportConfiguration;
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
import net.java.ao.DatabaseProvider;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import static com.atlassian.activeobjects.ao.ConverterUtils.*;
import static com.google.common.base.Preconditions.*;

final class PrefixedActiveObjectsBackup implements ActiveObjectsBackup
{
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final String NAMESPACE = "http://www.atlassian.com/ao";

    private final DatabaseProvider databaseProvider;
    private final PrefixedSchemaConfigurationFactory schemaConfigurationFactory;
    private final Prefix prefix;

    PrefixedActiveObjectsBackup(DatabaseProvider databaseProvider,
                                PrefixedSchemaConfigurationFactory schemaConfigurationFactory,
                                Prefix prefix)
    {
        this.databaseProvider = checkNotNull(databaseProvider);
        this.schemaConfigurationFactory = checkNotNull(schemaConfigurationFactory);
        this.prefix = checkNotNull(prefix);
    }

    /**
     * Saves the backup to an output stream.
     *
     * @param stream the stream to write the backup to
     * @param monitor the progress monitor for the current backup
     * @param pluginInfo
     * @throws com.atlassian.dbexporter.ImportExportException or one of its sub-types if any error happens during the backup.
     * {@link java.sql.SQLException SQL exceptions} will be wrapped in {@link com.atlassian.dbexporter.jdbc.ImportExportSqlException}.
     */
    public void save(OutputStream stream, BackupProgressMonitor monitor, PluginInformation pluginInfo)
    {
        final DatabaseProviderConnectionProvider connectionProvider = getConnectionProvider(databaseProvider);
        final ExportConfiguration configuration = new ActiveObjectsExportConfiguration(connectionProvider, getProgressMonitor(monitor));

        final DbExporter dbExporter = new DbExporter(
                new DatabaseInformationExporter(new ConnectionProviderInformationReader(connectionProvider), new PluginInformationReader(pluginInfo)),
                new TableDefinitionExporter(new ActiveObjectsTableReader(databaseProvider, schemaConfigurationFactory.getSchemaConfiguration(prefix))),
                new DataExporter(databaseProvider.getSchema(), new PrefixTableSelector(prefix)));

        NodeStreamWriter streamWriter = null;
        try
        {
            streamWriter = new StaxStreamWriter(new OutputStreamWriter(stream, CHARSET), CHARSET, NAMESPACE);
            dbExporter.exportData(streamWriter, configuration);
            streamWriter.flush();
        }
        finally
        {
            closeCloseable(streamWriter);
        }
    }

    /**
     * Restores the backup coming from the given input stream.
     *
     * @param stream the stream of data previously backed up by the plugin.
     * @param monitor the progress monitor for the current restore
     * @param pluginInformationChecker ...
     * @throws com.atlassian.dbexporter.ImportExportException or one of its sub-types if any error happens during the backup.
     * {@link java.sql.SQLException SQL exceptions} will be wrapped in {@link com.atlassian.dbexporter.jdbc.ImportExportSqlException}.
     */
    public void restore(InputStream stream, RestoreProgressMonitor monitor, PluginInformationChecker pluginInformationChecker)
    {
        final DatabaseProviderConnectionProvider connectionProvider = getConnectionProvider(databaseProvider);

        final DatabaseInformation databaseInformation = getDatabaseInformation(connectionProvider);

        final ImportConfiguration configuration = new ActiveObjectsImportConfiguration(connectionProvider, getProgressMonitor(monitor), databaseInformation);

        final DbImporter dbImporter = new DbImporter(
                new DatabaseInformationImporter(pluginInformationChecker),
                new TableDefinitionImporter(new ActiveObjectsTableCreator(databaseProvider), new ActiveObjectsDatabaseCleaner(databaseProvider, schemaConfigurationFactory.getSchemaConfiguration(prefix))),
                new DataImporter(
                        databaseProvider.getSchema(),
                        new SqlServerAroundTableImporter(),
                        new PostgresSequencesAroundImporter(databaseProvider),
                        new OracleSequencesAroundImporter(databaseProvider),
                        new ForeignKeyAroundImporter(new ActiveObjectsForeignKeyCreator(databaseProvider))
                ));

        NodeStreamReader streamReader = null;
        try
        {
            streamReader = new StaxStreamReader(new InputStreamReader(stream, CHARSET));
            dbImporter.importData(streamReader, configuration);
        }
        finally
        {
            closeCloseable(streamReader);
        }
    }

    private DatabaseInformation getDatabaseInformation(DatabaseProviderConnectionProvider connectionProvider)
    {
        return new DatabaseInformation(new ConnectionProviderInformationReader(connectionProvider).get());
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

    private static final class UpperCaseEntityNameProcessor implements EntityNameProcessor
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
