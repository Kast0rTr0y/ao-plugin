package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.node.NodeParser;

import java.util.Map;

import static com.atlassian.dbexporter.importer.ImporterUtils.*;
import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Maps.*;

public final class DatabaseInformationImporter extends AbstractSingleNodeImporter
{
    private DatabaseInformationChecker infoChecker;

    public DatabaseInformationImporter()
    {
        this(NoOpDatabaseInformationChecker.INSTANCE);
    }

    public DatabaseInformationImporter(DatabaseInformationChecker infoChecker)
    {
        this.infoChecker = checkNotNull(infoChecker);
    }

    @Override
    protected void doImportNode(NodeParser node, Context context)
    {
        final DatabaseInformation info = doImportDatabaseInformation(node);
        infoChecker.check(info, context);
        context.put(info);
    }

    private DatabaseInformation doImportDatabaseInformation(NodeParser node)
    {
        final Map<String, String> meta = newHashMap();
        checkStartNode(node, DatabaseInformationNode.NAME);
        doImportMetas(node.getNextNode(), meta);
        checkEndNode(node, DatabaseInformationNode.NAME);
        node.getNextNode(); // done with the database information
        return new DatabaseInformation(meta);
    }

    private void doImportMetas(NodeParser node, Map<String, String> meta)
    {
        while (node.getName().equals(DatabaseInformationNode.META))
        {
            doImportMeta(node, meta);
        }
    }

    private void doImportMeta(NodeParser node, Map<String, String> meta)
    {
        checkStartNode(node, DatabaseInformationNode.META);
        meta.put(DatabaseInformationNode.getMetaKey(node), DatabaseInformationNode.getMetaValue(node));
        checkEndNode(node.getNextNode(), DatabaseInformationNode.META);
        node.getNextNode(); // go to next node
    }

    @Override
    protected String getNodeName()
    {
        return DatabaseInformationNode.NAME;
    }

    private static final class NoOpDatabaseInformationChecker implements DatabaseInformationChecker
    {
        private static final DatabaseInformationChecker INSTANCE = new NoOpDatabaseInformationChecker();

        private NoOpDatabaseInformationChecker()
        {
        }

        public void check(DatabaseInformation information, Context context)
        {
            // no-op
        }
    }
}