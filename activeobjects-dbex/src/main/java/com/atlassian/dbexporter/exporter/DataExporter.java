package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ImportExportException;
import com.atlassian.dbexporter.jdbc.ImportExportSqlException;
import com.atlassian.dbexporter.jdbc.JdbcUtils;
import com.atlassian.dbexporter.node.NodeCreator;
import com.atlassian.dbexporter.progress.ProgressMonitor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
                for (String table : getTableNames(connection))
                {
                    exportTable(table, connection, node, monitor, configuration.getEntityNameProcessor());
                }
                node.closeEntity();
                return null;
            }
        });
        monitor.end(Task.TABLES_DATA);
    }

    /**
     * Returns the names of the tables that must be included in the export.
     *
     * @param connection the sql connection
     * @return the table names
     */
    private Set<String> getTableNames(Connection connection)
    {
        final Set<String> tables = new HashSet<String>();
        ResultSet result = getTablesResultSet(connection);
        try
        {
            String tableName;
            do
            {
                tableName = tableName(result);
                if (tableSelector.accept(tableName))
                {
                    tables.add(tableName);
                }
            }
            while (tableName != null);

            return tables;
        }
        finally
        {
            closeQuietly(result);
        }
    }

    private NodeCreator exportTable(String table, Connection connection, NodeCreator node, ProgressMonitor monitor, EntityNameProcessor entityNameProcessor)
    {
        monitor.begin(Task.TABLE_DATA, entityNameProcessor.tableName(table));
        TableDataNode.add(node, entityNameProcessor.tableName(table));

        final Statement statement = createStatement(connection);
        ResultSet result = null;
        try
        {
            result = executeQueryWithFetchSize(statement, "SELECT * FROM " + quote(connection, table), 100);
            final ResultSetMetaData meta = resultSetMetaData(result);

            // write column definitions
            node = writeColumnDefinitions(node, meta, entityNameProcessor);
            while (next(result))
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

    private NodeCreator exportRow(NodeCreator node, ResultSet result, ProgressMonitor monitor)
    {
        monitor.begin(Task.TABLE_ROW);
        final ResultSetMetaData metaData = resultSetMetaData(result);

        RowDataNode.add(node);

        for (int col = 1; col <= columnCount(metaData); col++)
        {
            switch (columnType(metaData, col))
            {
                case Types.BIGINT:
                case Types.INTEGER:
                    appendInteger(result, col, node);
                    break;
                case Types.NUMERIC:
                    if (scale(metaData, col) > 0) // oracle uses numeric always
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
                    RowDataNode.append(node, getString(result, col));
                    break;

                case Types.BOOLEAN:
                case Types.BIT:
                    RowDataNode.append(node, wasNull(result) ? null : getBoolean(result, col));
                    break;

                case Types.DOUBLE:
                case Types.DECIMAL:
                    appendDouble(result, col, node);
                    break;

                case Types.TIMESTAMP:
                    RowDataNode.append(node, getTimestamp(result, col));
                    break;

                default:
                    throw new ImportExportException(String.format(
                            "Cannot encode value for unsupported column type: \"%s\" (%d) of column %s.%s",
                            columnTypeName(metaData, col),
                            columnType(metaData, col),
                            tableName(metaData, col),
                            columnName(metaData, col)));
            }
        }

        monitor.end(Task.TABLE_ROW);
        return node.closeEntity();
    }

    private static void appendInteger(ResultSet result, int col, NodeCreator node)
    {
        RowDataNode.append(node, getBigDecimal(result, col).toBigInteger());
    }

    private static void appendDouble(ResultSet result, int col, NodeCreator node)
    {
        RowDataNode.append(node, BigDecimal.valueOf(getDouble(result, col)));
    }

    private NodeCreator writeColumnDefinitions(NodeCreator node, ResultSetMetaData metaData, EntityNameProcessor entityNameProcessor)
    {
        for (int i = 1; i <= columnCount(metaData); i++)
        {
            final String columnName = entityNameProcessor.columnName(columnName(metaData, i));
            ColumnDataNode.add(node, columnName).closeEntity();
        }
        return node;
    }

    private static ResultSetMetaData resultSetMetaData(ResultSet result)
    {
        try
        {
            return result.getMetaData();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get result set metadata", e);
        }
    }

    private static String tableName(ResultSet rs)
    {
        try
        {
            return next(rs) ? rs.getString("TABLE_NAME") : null;
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get table name from result set", e);
        }
    }

    private static int scale(ResultSetMetaData metaData, int col)
    {
        try
        {
            return metaData.getScale(col);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get scale for col #" + col + " from result set meta data", e);
        }
    }

    private static int columnCount(ResultSetMetaData metaData)
    {
        try
        {
            return metaData.getColumnCount();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get column count from result set metadata", e);
        }
    }

    private static int columnType(ResultSetMetaData metaData, int col)
    {
        try
        {
            return metaData.getColumnType(col);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get column type for col #" + col + " from result set meta data", e);
        }
    }

    private static String columnTypeName(ResultSetMetaData metaData, int col)
    {
        try
        {
            return metaData.getColumnTypeName(col);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get column type name for col #" + col + " from result set meta data", e);
        }
    }

    private static String columnName(ResultSetMetaData metaData, int i)
    {
        try
        {
            return metaData.getColumnName(i);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get column #" + i + " name from result set meta data", e);
        }
    }

    private static String tableName(ResultSetMetaData metaData, int col)
    {
        try
        {
            return metaData.getTableName(col);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get table name for col #" + col + " from result set meta data", e);
        }
    }

    private static String getString(ResultSet result, int col)
    {
        try
        {
            return result.getString(col);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get string value for col #" + col, e);
        }
    }

    private static boolean getBoolean(ResultSet result, int col)
    {
        try
        {
            return result.getBoolean(col);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get boolean value for col #" + col, e);
        }
    }

    private static BigDecimal getBigDecimal(ResultSet result, int col)
    {
        try
        {
            return result.getBigDecimal(col);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get big decimal value for col #" + col, e);
        }
    }

    private static double getDouble(ResultSet result, int col)
    {
        try
        {
            return result.getDouble(col);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get double value for col #" + col, e);
        }
    }

    private static Timestamp getTimestamp(ResultSet result, int col)
    {
        try
        {
            return result.getTimestamp(col);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get timestamp value for col #" + col, e);
        }
    }

    private static boolean wasNull(ResultSet result)
    {
        try
        {
            return result.wasNull();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not figure out whether value was NULL", e);
        }
    }

    private static boolean next(ResultSet result)
    {
        try
        {
            return result.next();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get next for result set", e);
        }
    }

    private static ResultSet getTablesResultSet(Connection connection)
    {
        try
        {
            return metadata(connection).getTables(null, null, "%", new String[]{"TABLE"});
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not read tables in data exporter", e);
        }
    }

    private static ResultSet executeQueryWithFetchSize(Statement statement, String sql, int fetchSize)
    {
        try
        {
            statement.setFetchSize(fetchSize);
            return statement.executeQuery(sql);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not execute query '" + sql + "' with fetch size " + fetchSize, e);
        }
    }
}
