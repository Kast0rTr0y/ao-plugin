package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.node.NodeCreator;

import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.google.common.base.Preconditions.checkNotNull;

public final class TableDefinitionExporter implements Exporter
{
    private final TableReader tableReader;

    public TableDefinitionExporter(TableReader tableReader)
    {
        this.tableReader = checkNotNull(tableReader);
    }

    @Override
    public void export(NodeCreator node, Context context)
    {
        for (Table table : tableReader.read(context))
        {
            export(node, table);
        }
    }

    private void export(NodeCreator node, Table table)
    {
        TableDefinitionNode.add(node);
        TableDefinitionNode.setName(node, table.getName());

        for (Column column : table.getColumns())
        {
            export(node, column);
        }

        for (ForeignKey foreignKey : table.getForeignKeys())
        {
            export(node, foreignKey);
        }

        node.closeEntity();
    }

    private void export(NodeCreator node, Column column)
    {
        ColumnDefinitionNode.add(node);
        ColumnDefinitionNode.setName(node, column.getName());
        ColumnDefinitionNode.setPrimaryKey(node, column.isPrimaryKey());
        ColumnDefinitionNode.setAutoIncrement(node, column.isAutoIncrement());
        ColumnDefinitionNode.setSqlType(node, column.getSqlType());
        ColumnDefinitionNode.setPrecision(node, column.getPrecision());
        node.closeEntity();
    }

    private void export(NodeCreator node, ForeignKey foreignKey)
    {
        ForeignKeyDefinitionNode.add(node, foreignKey.getName());
        ForeignKeyDefinitionNode.setFromTable(node, foreignKey.getFromTable());
        ForeignKeyDefinitionNode.setFromColumn(node, foreignKey.getFromField());
        ForeignKeyDefinitionNode.setToTable(node, foreignKey.getToTable());
        ForeignKeyDefinitionNode.setToColumn(node, foreignKey.getToField());
        node.closeEntity();
    }
}
