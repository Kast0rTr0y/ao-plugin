package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import com.atlassian.dbexporter.progress.Update;
import com.atlassian.dbexporter.node.NodeParser;

import java.util.Collection;
import java.util.List;

import static com.atlassian.dbexporter.ContextUtils.getProgressMonitor;
import static com.atlassian.dbexporter.importer.ImporterUtils.*;
import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

public final class TableDefinitionImporter extends AbstractSingleNodeImporter
{
    private final TableCreator tableCreator;

    public TableDefinitionImporter(TableCreator tableCreator)
    {
        this.tableCreator = checkNotNull(tableCreator);
    }

    @Override
    protected void doImportNode(NodeParser node, Context context)
    {
        final ProgressMonitor monitor = getProgressMonitor(context);

        monitor.update(Update.from("Creating table definitions..."));

        final List<Table> tables = newArrayList();
        while (!node.isClosed() && node.getName().equals(getNodeName()))
        {
            tables.add(readTable(node));
        }

        tableCreator.create(tables, context);
        context.putAll(tables); // add the parsed tables to the context
    }

    private Table readTable(NodeParser node)
    {
        checkStartNode(node, TableDefinitionNode.NAME);

        final String tableName = TableDefinitionNode.getName(node);
        node.getNextNode();

        final List<Column> columns = readColumns(node);
        final Collection<ForeignKey> foreignKeys = readForeignKeys(node);

        checkEndNode(node, TableDefinitionNode.NAME);

        node.getNextNode(); // get to the next node, that table has been imported!

        return new Table(tableName, columns, foreignKeys);
    }

    private List<Column> readColumns(NodeParser node)
    {
        final List<Column> columns = newArrayList();
        while (node.getName().equals(ColumnDefinitionNode.NAME))
        {
            columns.add(readColumn(node));
        }
        return columns;
    }

    private Column readColumn(NodeParser node)
    {
        checkStartNode(node, ColumnDefinitionNode.NAME);

        final String columnName = ColumnDefinitionNode.getName(node);
        final boolean isPk = ColumnDefinitionNode.isPrimaryKey(node);
        final boolean isAi= ColumnDefinitionNode.isAutoIncrement(node);
        final int sqlType = ColumnDefinitionNode.getSqlType(node);
        final Integer precision = ColumnDefinitionNode.getPrecision(node);

        checkEndNode(node.getNextNode(), ColumnDefinitionNode.NAME);
        node.getNextNode(); // get to the next node, that column has been imported!
        return new Column(columnName, sqlType, isPk, isAi, precision);
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
}
