package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.node.NodeParser;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

import static com.atlassian.dbexporter.importer.ImporterUtils.*;
import static com.atlassian.dbexporter.importer.TableDefinitionImporter.DatabaseCleanerAroundImporter.*;
import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.atlassian.dbexporter.progress.ProgressMonitor.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

public final class TableDefinitionImporter extends AbstractSingleNodeImporter
{
    private final TableCreator tableCreator;

    public TableDefinitionImporter(ImportExportErrorService errorService, TableCreator tableCreator, final DatabaseCleaner databaseCleaner)
    {
        super(errorService, Lists.<AroundImporter>newArrayList(newCleaner(databaseCleaner)));
        this.tableCreator = checkNotNull(tableCreator);
    }

    @Override
    protected void doImportNode(NodeParser node, ImportConfiguration configuration, Context context)
    {
        final ProgressMonitor monitor = configuration.getProgressMonitor();
        monitor.begin(Task.TABLE_DEFINITION);

        final List<Table> tables = newArrayList();
        while (isNodeNotClosed(node, getNodeName()))
        {
            tables.add(readTable(node, configuration.getEntityNameProcessor()));
        }

        monitor.end(Task.TABLE_DEFINITION);
        monitor.totalNumberOfTables(tables.size());

        tableCreator.create(tables, configuration.getEntityNameProcessor(), monitor);
        context.putAll(tables); // add the parsed tables to the context
    }

    private Table readTable(NodeParser node, EntityNameProcessor entityNameProcessor)
    {
        checkStartNode(node, TableDefinitionNode.NAME);

        final String tableName = entityNameProcessor.tableName(TableDefinitionNode.getName(node));
        node.getNextNode();

        final List<Column> columns = readColumns(node, entityNameProcessor);
        final Collection<ForeignKey> foreignKeys = readForeignKeys(node);

        checkEndNode(node, TableDefinitionNode.NAME);

        node.getNextNode(); // get to the next node, that table has been imported!

        return new Table(tableName, columns, foreignKeys);
    }

    private List<Column> readColumns(NodeParser node, EntityNameProcessor entityNameProcessor)
    {
        final List<Column> columns = newArrayList();
        while (node.getName().equals(ColumnDefinitionNode.NAME))
        {
            columns.add(readColumn(node, entityNameProcessor));
        }
        return columns;
    }

    private Column readColumn(NodeParser node, EntityNameProcessor entityNameProcessor)
    {
        checkStartNode(node, ColumnDefinitionNode.NAME);

        final String columnName = entityNameProcessor.columnName(ColumnDefinitionNode.getName(node));
        final boolean isPk = ColumnDefinitionNode.isPrimaryKey(node);
        final boolean isAi = ColumnDefinitionNode.isAutoIncrement(node);
        final int sqlType = ColumnDefinitionNode.getSqlType(node);
        final Integer precision = ColumnDefinitionNode.getPrecision(node);
        final Integer scale = ColumnDefinitionNode.getScale(node);

        checkEndNode(node.getNextNode(), ColumnDefinitionNode.NAME);
        node.getNextNode(); // get to the next node, that column has been imported!
        return new Column(columnName, sqlType, isPk, isAi, precision, scale);
    }

    private Collection<ForeignKey> readForeignKeys(NodeParser node)
    {
        final Collection<ForeignKey> fks = newArrayList();
        while (node.getName().equals(ForeignKeyDefinitionNode.NAME))
        {
            fks.add(readForeignKey(node));
        }
        return fks;
    }

    private ForeignKey readForeignKey(NodeParser node)
    {
        checkStartNode(node, ForeignKeyDefinitionNode.NAME);

        final String name = ForeignKeyDefinitionNode.getName(node);
        final String fromTable = ForeignKeyDefinitionNode.getFromTable(node);
        final String fromColumn = ForeignKeyDefinitionNode.getFromColumn(node);
        final String toTable = ForeignKeyDefinitionNode.getToTable(node);
        final String toColumn = ForeignKeyDefinitionNode.getToColumn(node);

        checkEndNode(node.getNextNode(), ForeignKeyDefinitionNode.NAME);
        node.getNextNode(); // get to the next node, that column has been imported!
        return new ForeignKey(name, fromTable, fromColumn, toTable, toColumn);
    }

    @Override
    protected String getNodeName()
    {
        return TableDefinitionNode.NAME;
    }

    static final class DatabaseCleanerAroundImporter extends NoOpAroundImporter
    {
        private final DatabaseCleaner databaseCleaner;

        private DatabaseCleanerAroundImporter(DatabaseCleaner databaseCleaner)
        {
            this.databaseCleaner = checkNotNull(databaseCleaner);
        }

        @Override
        public void before(NodeParser node, ImportConfiguration configuration, Context context)
        {
            databaseCleaner.cleanup(configuration.getCleanupMode());
        }

        static DatabaseCleanerAroundImporter newCleaner(DatabaseCleaner cleaner)
        {
            return new DatabaseCleanerAroundImporter(cleaner);
        }
    }
}
