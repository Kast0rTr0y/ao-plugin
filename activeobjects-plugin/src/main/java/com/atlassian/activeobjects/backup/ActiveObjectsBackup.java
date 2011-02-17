package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.ao.PrefixedSchemaConfiguration;
import com.atlassian.activeobjects.internal.DatabaseProviderFactory;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.SimplePrefix;
import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.dbexporter.DbExporter;
import com.atlassian.dbexporter.DbImporter;
import com.atlassian.dbexporter.exporter.DataExporter;
import com.atlassian.dbexporter.exporter.DatabaseInformationExporter;
import com.atlassian.dbexporter.exporter.TableDefinitionExporter;
import com.atlassian.dbexporter.importer.DataImporter;
import com.atlassian.dbexporter.importer.DatabaseInformationImporter;
import com.atlassian.dbexporter.importer.TableDefinitionImporter;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import com.atlassian.dbexporter.progress.Slf4jProgressMonitor;
import com.atlassian.dbexporter.node.NodeStreamReader;
import com.atlassian.dbexporter.node.NodeStreamWriter;
import com.atlassian.dbexporter.node.stax.StaxStreamReader;
import com.atlassian.dbexporter.node.stax.StaxStreamWriter;
import net.java.ao.DatabaseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsBackup implements Backup
{
    public static final Prefix PREFIX = new SimplePrefix("AO");

    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final String NAMESPACE = "http://www.atlassian.com/ao";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DatabaseProvider databaseProvider;

    public ActiveObjectsBackup(DatabaseProviderFactory databaseProviderFactory, DataSourceProvider dataSourceProvider)
    {
        this(checkNotNull(databaseProviderFactory).getDatabaseProvider(dataSourceProvider.getDataSource(), dataSourceProvider.getDatabaseType()));
    }

    ActiveObjectsBackup(DatabaseProvider databaseProvider)
    {
        this.databaseProvider = checkNotNull(databaseProvider);
    }

    public void save(OutputStream stream)
    {
        final DbExporter dbExporter = new DbExporter(
                getProgressMonitor(),
                new DatabaseInformationExporter(new DatabaseProviderInformationExporter(databaseProvider)),
                new TableDefinitionExporter(new ActiveObjectsTableReader(databaseProvider, new PrefixedSchemaConfiguration(PREFIX))),
                new DataExporter(new PrefixTableSelector(PREFIX)));


        NodeStreamWriter streamWriter = null;
        try
        {
            streamWriter = new StaxStreamWriter(new OutputStreamWriter(stream, CHARSET), CHARSET, NAMESPACE);
            dbExporter.exportData(streamWriter, new DatabaseProviderConnectionProvider(databaseProvider));
            streamWriter.flush();
        }
        finally
        {
            closeCloseable(streamWriter);
        }
    }

    public void restore(InputStream stream)
    {
        final DbImporter dbImporter = new DbImporter(
                getProgressMonitor(),
                new DatabaseInformationImporter(),
                new TableDefinitionImporter(new ActiveObjectsTableCreator(databaseProvider)),
                new DataImporter(new ForeignKeyAroundImporter(new ActiveObjectsForeignKeyCreator(databaseProvider))));


        NodeStreamReader streamReader = null;
        try
        {
            streamReader = new StaxStreamReader(new InputStreamReader(stream, CHARSET));
            dbImporter.importData(streamReader, new DatabaseProviderConnectionProvider(databaseProvider));
        }
        finally
        {
            closeCloseable(streamReader);
        }
    }

    private ProgressMonitor getProgressMonitor()
    {
        return new Slf4jProgressMonitor(logger);
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
}
