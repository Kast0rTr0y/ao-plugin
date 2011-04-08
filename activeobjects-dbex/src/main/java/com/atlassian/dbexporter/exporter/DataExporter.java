package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.jdbc.JdbcUtils;
import com.atlassian.dbexporter.jdbc.SqlRuntimeException;
import com.atlassian.dbexporter.node.NodeCreator;
import com.atlassian.dbexporter.node.ParseException;
import com.atlassian.dbexporter.progress.ProgressMonitor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.atlassian.dbexporter.progress.ProgressMonitor.*;
import static com.google.common.base.Preconditions.*;

public final class DataExporter implements Exporter
{
    private final TableSelector tableSelector;

    public DataExporter(TableSelector tableSelector)
    {
        this.tableSelector = checkNotNull(tableSelector);
    }

    @Override
    public void export(final NodeCreator node, final ExportConfiguration configuration, final Context context)
    {
        final ProgressMonitor monitor = configuration.getProgressMonitor();
        monitor.begin(Task.TABLES_DATA);
        withConnection(configuration.getConnectionProvider(), new JdbcUtils.JdbcCallable<Void>()
        {
            public Void call(Connection connection)
            {
                try
                {
                    final Set<String> allTables = getTableNames(connection);
                    for (String table : allTables)
                    {
                        exportTable(table, connection, node, monitor, configuration.getEntityNameProcessor());
                    }
                    node.closeEntity();
                }
                catch (SQLException e)
                {
                    throw new SqlRuntimeException(e);
                }
                return null;
            }
        });
        monitor.end(Task.TABLES_DATA);
    }

    /**
     * Returns the names of the tables that must be included in the export.
     *
     * @param connection the sql connection
     */
    private Set<String> getTableNames(Connection connection) throws SQLException
    {
        final Set<String> tables = new HashSet<String>();
        ResultSet result = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
        try
        {
            while (result.next())
            {
                final String tableName = result.getString("TABLE_NAME");
                if (tableSelector.accept(tableName))
                {
                    tables.add(tableName);
                }
            }
            return tables;
        }
        finally
        {
            closeQuietly(result);
        }
    }

    private NodeCreator exportTable(String table, Connection connection, NodeCreator node, ProgressMonitor monitor, EntityNameProcessor entityNameProcessor) throws SQLException, ParseException
    {
        monitor.begin(Task.TABLE_DATA, entityNameProcessor.tableName(table));
        TableDataNode.add(node, entityNameProcessor.tableName(table));

        Statement statement = connection.createStatement();
        ResultSet result = null;
        try
        {
            statement.setFetchSize(100);
            result = statement.executeQuery("SELECT * FROM " + quote(connection, table));
            final ResultSetMetaData meta = result.getMetaData();

            // write column definitions
            node = writeColumnDefinitions(node, meta, entityNameProcessor);

            while (result.next())
            {
                node = exportRow(node, result, monitor);
            }
        }
        finally
        {
            closeQuietly(result, statement);
        }

        monitor.end(Task.TABLE_DATA, entityNameProcessor.tableName(table));
        return node.closeEntity();
    }

    private NodeCreator exportRow(NodeCreator node, ResultSet result, ProgressMonitor monitor) throws ParseException, SQLException
    {
        monitor.begin(Task.TABLE_ROW);
        final ResultSetMetaData metaData = result.getMetaData();
        final int columns = metaData.getColumnCount();

        RowDataNode.add(node);

        for (int col = 1; col <= columns; col++)
        {
            switch (metaData.getColumnType(col))
            {
                case Types.BIGINT:
                case Types.INTEGER:
                    appendInteger(result, col, node);
                    break;
                case Types.NUMERIC:
                    if (metaData.getScale(col) > 0) // oracle uses numeric always
                    {
                        appendDouble(result, col, node);
                    }
                    else
                    {
                        appendInteger(result, col, node);
                    }
                    break;
                case Types.VARCHAR:
                case Types.LONGVARCHAR:
                    RowDataNode.append(node, result.getString(col));
                    break;

                case Types.BOOLEAN:
                case Types.BIT:
                    RowDataNode.append(node, result.wasNull() ? null : result.getBoolean(col));
                    break;

                case Types.DOUBLE:
                    appendDouble(result, col, node);
                    break;

                case Types.TIMESTAMP:
                    RowDataNode.append(node, result.getTimestamp(col));
                    break;

                default:
                    throw new SQLException(String.format(
                            "Cannot encode value for unsupported column type: \"%s\" (%d) of column %s.%s",
                            metaData.getColumnTypeName(col), metaData.getColumnType(col),
                            metaData.getTableName(col), metaData.getColumnName(col)));
            }
        }

        monitor.end(Task.TABLE_ROW);
        return node.closeEntity();
    }

    private static void appendInteger(ResultSet result, int col, NodeCreator node) throws SQLException
    {
        RowDataNode.append(node, result.getBigDecimal(col).toBigInteger());
    }

    private static void appendDouble(ResultSet result, int col, NodeCreator node) throws SQLException
    {
        RowDataNode.append(node, BigDecimal.valueOf(result.getDouble(col)));
    }

    private NodeCreator writeColumnDefinitions(NodeCreator node, ResultSetMetaData metaData, EntityNameProcessor entityNameProcessor) throws SQLException, ParseException
    {
        for (int i = 1; i <= metaData.getColumnCount(); i++)
        {
            final String columnName = entityNameProcessor.columnName(metaData.getColumnName(i));
            ColumnDataNode.add(node, columnName).closeEntity();
        }
        return node;
    }
}
