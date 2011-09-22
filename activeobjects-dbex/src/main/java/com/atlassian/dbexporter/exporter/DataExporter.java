package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.jdbc.JdbcUtils;
import com.atlassian.dbexporter.node.NodeCreator;
import com.atlassian.dbexporter.progress.ProgressMonitor;

import java.math.BigDecimal;
import java.sql.Clob;
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
    private final ImportExportErrorService errorService;

    private final String schema;
    private final TableSelector tableSelector;

    public DataExporter(ImportExportErrorService errorService, String schema, TableSelector tableSelector)
    {
        this.errorService = checkNotNull(errorService);
        this.schema = isBlank(schema) ? null : schema; // maybe null
        this.tableSelector = checkNotNull(tableSelector);
    }

    @Override
    public void export(final NodeCreator node, final ExportConfiguration configuration, final Context context)
    {
        final ProgressMonitor monitor = configuration.getProgressMonitor();
        monitor.begin(Task.TABLES_DATA);
        withConnection(errorService, configuration.getConnectionProvider(), new JdbcUtils.JdbcCallable<Void>()
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

        final Statement statement = createStatement(errorService, table, connection);
        ResultSet result = null;
        try
        {
            result = executeQueryWithFetchSize(table, statement, "SELECT * FROM " + tableName(table, connection), 100);
            final ResultSetMetaData meta = resultSetMetaData(table, result);

            // write column definitions
            node = writeColumnDefinitions(table, node, meta, entityNameProcessor);
            while (next(table, result))
            {
                node = exportRow(table, node, result, monitor);
            }
        }
        finally
        {
            closeQuietly(result, statement);
        }

        monitor.end(Task.TABLE_DATA, entityNameProcessor.tableName(table));
        return node.closeEntity();
    }

    private String tableName(String table, Connection connection)
    {
        final String quoted = quote(errorService, table, connection, table);
        return schema != null ? schema + "." + quoted : quoted;
    }

    private NodeCreator exportRow(String table, NodeCreator node, ResultSet result, ProgressMonitor monitor)
    {
        monitor.begin(Task.TABLE_ROW);
        final ResultSetMetaData metaData = resultSetMetaData(table, result);

        RowDataNode.add(node);

        for (int col = 1; col <= columnCount(table, metaData); col++)
        {
            switch (columnType(table, metaData, col))
            {
                case Types.BIGINT:
                case Types.INTEGER:
                    appendInteger(table, result, col, node);
                    break;
                case Types.NUMERIC:
                    if (scale(table, metaData, col) > 0) // oracle uses numeric always
                    {
                        appendDouble(table, result, col, node);
                    }
                    else
                    {
                        appendInteger(table, result, col, node);
                    }
                    break;
                case Types.VARCHAR:
                case Types.LONGVARCHAR:
                    final String s = getString(table, result, col);
                    RowDataNode.append(node, wasNull(table, result) ? null : s);
                    break;

                case Types.BOOLEAN:
                case Types.BIT:
                    final boolean b = getBoolean(table, result, col);
                    RowDataNode.append(node, wasNull(table, result) ? null : b);
                    break;

                case Types.DOUBLE:
                case Types.DECIMAL:
                    appendDouble(table, result, col, node);
                    break;

                case Types.TIMESTAMP:
                    final Timestamp t = getTimestamp(table, result, col);
                    RowDataNode.append(node, wasNull(table, result) ? null : t);
                    break;

                case Types.CLOB:
                    final String c = getClobAsString(table, result, col);
                    RowDataNode.append(node, wasNull(table, result) ? null : c);
                    break;

                default:
                    throw errorService.newImportExportException(table, String.format(
                            "Cannot encode value for unsupported column type: \"%s\" (%d) of column %s.%s",
                            columnTypeName(table, metaData, col),
                            columnType(table, metaData, col),
                            table,
                            columnName(table, metaData, col)));
            }
        }

        monitor.end(Task.TABLE_ROW);
        return node.closeEntity();
    }

    private void appendInteger(String table, ResultSet result, int col, NodeCreator node)
    {
        final BigDecimal bd = getBigDecimal(table, result, col);
        RowDataNode.append(node, wasNull(table, result) ? null : bd.toBigInteger());
    }

    private void appendDouble(String table, ResultSet result, int col, NodeCreator node)
    {
        final double d = getDouble(table, result, col);
        RowDataNode.append(node, wasNull(table, result) ? null : BigDecimal.valueOf(d));
    }

    private NodeCreator writeColumnDefinitions(String table, NodeCreator node, ResultSetMetaData metaData, EntityNameProcessor entityNameProcessor)
    {
        for (int i = 1; i <= columnCount(table, metaData); i++)
        {
            final String columnName = entityNameProcessor.columnName(columnName(table, metaData, i));
            ColumnDataNode.add(node, columnName).closeEntity();
        }
        return node;
    }

    private ResultSetMetaData resultSetMetaData(String table, ResultSet result)
    {
        try
        {
            return result.getMetaData();
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get result set metadata", e);
        }
    }

    private String tableName(ResultSet rs)
    {
        try
        {
            return next(null, rs) ? rs.getString("TABLE_NAME") : null;
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "Could not get table name from result set", e);
        }
    }

    private int scale(String table, ResultSetMetaData metaData, int col)
    {
        try
        {
            return metaData.getScale(col);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get scale for col #" + col + " from result set meta data", e);
        }
    }

    private int columnCount(String table, ResultSetMetaData metaData)
    {
        try
        {
            return metaData.getColumnCount();
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get column count from result set metadata", e);
        }
    }

    private int columnType(String table, ResultSetMetaData metaData, int col)
    {
        try
        {
            return metaData.getColumnType(col);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get column type for col #" + col + " from result set meta data", e);
        }
    }

    private String columnTypeName(String table, ResultSetMetaData metaData, int col)
    {
        try
        {
            return metaData.getColumnTypeName(col);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get column type name for col #" + col + " from result set meta data", e);
        }
    }

    private String columnName(String table, ResultSetMetaData metaData, int i)
    {
        try
        {
            return metaData.getColumnName(i);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get column #" + i + " name from result set meta data", e);
        }
    }

    private String getString(String table, ResultSet result, int col)
    {
        try
        {
            return result.getString(col);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get string value for col #" + col, e);
        }
    }

    private boolean getBoolean(String table, ResultSet result, int col)
    {
        try
        {
            return result.getBoolean(col);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get boolean value for col #" + col, e);
        }
    }

    private BigDecimal getBigDecimal(String table, ResultSet result, int col)
    {
        try
        {
            return result.getBigDecimal(col);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get big decimal value for col #" + col, e);
        }
    }

    private double getDouble(String table, ResultSet result, int col)
    {
        try
        {
            return result.getDouble(col);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get double value for col #" + col, e);
        }
    }

    private Timestamp getTimestamp(String table, ResultSet result, int col)
    {
        try
        {
            return result.getTimestamp(col);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get timestamp value for col #" + col, e);
        }
    }

    private String getClobAsString(String table, ResultSet result, int col)
    {
        try
        {
            final Clob clob = result.getClob(col);
            return clob == null ? null : clob.getSubString(1L, (int) clob.length());
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get clob value for col #" + col, e);
        }
    }

    private boolean wasNull(String table, ResultSet result)
    {
        try
        {
            return result.wasNull();
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not figure out whether value was NULL", e);
        }
    }

    private boolean next(String table, ResultSet result)
    {
        try
        {
            return result.next();
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not get next for result set", e);
        }
    }

    private ResultSet getTablesResultSet(Connection connection)
    {
        try
        {
            return metadata(errorService, connection).getTables(null, schema, "%", new String[]{"TABLE"});
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "Could not read tables in data exporter", e);
        }
    }

    private ResultSet executeQueryWithFetchSize(String table, Statement statement, String sql, int fetchSize)
    {
        try
        {
            statement.setFetchSize(fetchSize);
            return statement.executeQuery(sql);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not execute query '" + sql + "' with fetch size " + fetchSize, e);
        }
    }

    private static boolean isBlank(String str)
    {
        int strLen;
        if (str == null || (strLen = str.length()) == 0)
        {
            return true;
        }
        for (int i = 0; i < strLen; i++)
        {
            if (!Character.isWhitespace(str.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }
}
