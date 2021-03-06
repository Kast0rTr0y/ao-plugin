package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.node.NodeCreator;
import com.atlassian.dbexporter.progress.ProgressMonitor;

import static com.atlassian.dbexporter.node.NodeBackup.ColumnDefinitionNode;
import static com.atlassian.dbexporter.node.NodeBackup.ForeignKeyDefinitionNode;
import static com.atlassian.dbexporter.node.NodeBackup.TableDefinitionNode;
import static com.atlassian.dbexporter.progress.ProgressMonitor.Task;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;

public final class TableDefinitionExporter implements Exporter {
    private final TableReader tableReader;

    public TableDefinitionExporter(TableReader tableReader) {
        this.tableReader = checkNotNull(tableReader);
    }

    @Override
    public void export(NodeCreator node, ExportConfiguration configuration, Context context) {
        final ProgressMonitor monitor = configuration.getProgressMonitor();
        monitor.begin(Task.TABLE_DEFINITION);

        int tableCount = 0;
        final Iterable<Table> tables = tableReader.read(getDatabaseInformation(context), configuration.getEntityNameProcessor());
        for (Table table : tables) {
            export(node, table);
            tableCount++;
        }
        monitor.end(Task.TABLE_DEFINITION);
        monitor.totalNumberOfTables(tableCount);

        context.putAll(newLinkedList(tables));
    }

    private DatabaseInformation getDatabaseInformation(Context context) {
        return checkNotNull(context.get(DatabaseInformation.class));
    }

    private void export(NodeCreator node, Table table) {
        TableDefinitionNode.add(node);
        TableDefinitionNode.setName(node, table.getName());

        for (Column column : table.getColumns()) {
            export(node, column);
        }

        for (ForeignKey foreignKey : table.getForeignKeys()) {
            export(node, foreignKey);
        }

        node.closeEntity();
    }

    private void export(NodeCreator node, Column column) {
        ColumnDefinitionNode.add(node);
        ColumnDefinitionNode.setName(node, column.getName());
        ColumnDefinitionNode.setPrimaryKey(node, column.isPrimaryKey());
        ColumnDefinitionNode.setAutoIncrement(node, column.isAutoIncrement());
        ColumnDefinitionNode.setSqlType(node, column.getSqlType());
        ColumnDefinitionNode.setPrecision(node, column.getPrecision());
        ColumnDefinitionNode.setScale(node, column.getScale());
        node.closeEntity();
    }

    private void export(NodeCreator node, ForeignKey foreignKey) {
        ForeignKeyDefinitionNode.add(node);
        ForeignKeyDefinitionNode.setFromTable(node, foreignKey.getFromTable());
        ForeignKeyDefinitionNode.setFromColumn(node, foreignKey.getFromField());
        ForeignKeyDefinitionNode.setToTable(node, foreignKey.getToTable());
        ForeignKeyDefinitionNode.setToColumn(node, foreignKey.getToField());
        node.closeEntity();
    }
}
