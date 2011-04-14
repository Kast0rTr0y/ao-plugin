package com.atlassian.dbexporter;

import com.atlassian.dbexporter.importer.ImportConfiguration;
import com.atlassian.dbexporter.importer.Importer;
import com.atlassian.dbexporter.importer.NoOpImporter;
import com.atlassian.dbexporter.jdbc.ImportExportSqlException;
import com.atlassian.dbexporter.node.NodeParser;
import com.atlassian.dbexporter.node.NodeStreamReader;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.atlassian.dbexporter.DatabaseInformations.database;
import static com.atlassian.dbexporter.importer.ImporterUtils.*;
import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

/**
 * <p>Loads the data from a platform-independent backup file into the database.</p>
 * <p>What data is actually imported in the database depends heavily on both, the backup provided, and also the
 * importers that are passed-in</p>
 *
 * @author Erik van Zijst
 * @author Samuel Le Berrigaud
 */
public final class DbImporter
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final List<Importer> importers;

    public DbImporter(final Importer... importers)
    {
        this(newArrayList(checkNotNull(importers)));
    }

    public DbImporter(final List<Importer> importers)
    {
        checkArgument(!checkNotNull(importers).isEmpty(), "DbImporter must be created with at least one importer!");
        this.importers = importers;
    }

    /**
     * Imports the XML document read from the stream
     *
     * @param streamReader the XML stream reader
     * @param configuration the import configuration
     * @throws IllegalStateException if the backup XML stream is not formatted as expected.
     * @throws ImportExportException or one of its sub-types if an unexpected exception happens during the import.
     * Note that {@link ImportExportSqlException} is the main sub-type of exception likely to be thrown whenever an
     * underlying {@link java.sql.SQLException} is thrown within one of the specific {@link Importer}.
     */
    public void importData(NodeStreamReader streamReader, ImportConfiguration configuration)
    {
        final ProgressMonitor monitor = configuration.getProgressMonitor();
        final DatabaseInformations.Database database = database(configuration.getDatabaseInformation());

        monitor.begin(database);

        final NodeParser node = RootNode.get(streamReader);
        logger.debug("Root node is {}", node);

        checkStartNode(node, RootNode.NAME);
        node.getNextNode();//  starting with the first node

        final Context context = new Context();

        logger.debug("Starting import from node {}", node);
        do
        {
            getImporter(node).importNode(node, configuration, context);
        }
        while (!(node.getName().equals(RootNode.NAME) && node.isClosed()));

        monitor.end(database);
    }

    private Importer getImporter(NodeParser node)
    {
        for (Importer importer : importers)
        {
            if (importer.supports(node))
            {
                logger.debug("Found importer {} for node {}", importer, node);
                return importer;
            }
        }
        logger.debug("Didn't find any importer for node {}, using {}", node, NoOpImporter.INSTANCE);
        return NoOpImporter.INSTANCE;
    }
}
