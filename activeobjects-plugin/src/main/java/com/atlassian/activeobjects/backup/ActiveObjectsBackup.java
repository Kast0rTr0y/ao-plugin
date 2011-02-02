package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.internal.DatabaseProviderFactory;
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
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final String NAMESPACE = "http://www.atlassian.com/ao";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DatabaseProviderFactory databaseProviderFactory;
    private final DataSourceProvider dataSourceProvider;

    public ActiveObjectsBackup(DatabaseProviderFactory databaseProviderFactory, DataSourceProvider dataSourceProvider)
    {
        this.databaseProviderFactory = checkNotNull(databaseProviderFactory);
        this.dataSourceProvider = checkNotNull(dataSourceProvider);
    }

    public void save(OutputStream stream)
    {
        final DatabaseProvider provider = getProvider();

        final DbExporter dbExporter = new DbExporter(
                getProgressMonitor(),
                new DatabaseInformationExporter(),
                new TableDefinitionExporter(new ActiveObjectsTableReader(provider)),
                new DataExporter());


        NodeStreamWriter streamWriter = null;
        try
        {
            streamWriter = new StaxStreamWriter(new OutputStreamWriter(stream, CHARSET), CHARSET, NAMESPACE);
            dbExporter.exportData(streamWriter, new DatabaseProviderConnectionProvider(provider));
            streamWriter.flush();
        }
        finally
        {
            closeCloseable(streamWriter);
        }
    }

    public void restore(InputStream stream)
    {
        final DatabaseProvider provider = getProvider();

        final DbImporter dbImporter = new DbImporter(
                getProgressMonitor(),
                new DatabaseInformationImporter(),
                new TableDefinitionImporter(new ActiveObjectsTableCreator(provider)),
                new DataImporter(new ForeignKeyAroundImporter(new ActiveObjectsForeignKeyCreator(provider))));


        NodeStreamReader streamReader = null;
        try
        {
            streamReader = new StaxStreamReader(new InputStreamReader(stream, CHARSET));
            dbImporter.importData(streamReader, new DatabaseProviderConnectionProvider(provider));
        }
        finally
        {
            closeCloseable(streamReader);
        }
    }

    private DatabaseProvider getProvider()
    {
        return databaseProviderFactory.getDatabaseProvider(dataSourceProvider.getDataSource(), dataSourceProvider.getDatabaseType());
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
