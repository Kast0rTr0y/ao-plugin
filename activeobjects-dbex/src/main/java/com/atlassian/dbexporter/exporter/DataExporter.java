package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.ConnectionProvider;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.jdbc.JdbcUtils;
import com.atlassian.dbexporter.jdbc.SqlRuntimeException;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import com.atlassian.dbexporter.progress.Update;
import com.atlassian.dbexporter.progress.Warning;
import com.atlassian.dbexporter.node.NodeCreator;
import com.atlassian.dbexporter.node.ParseException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.atlassian.dbexporter.ContextUtils.*;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Maps.*;

public final class DataExporter implements Exporter
{
    private final TableSelector tableSelector;

    public DataExporter(TableSelector tableSelector)
    {
        this.tableSelector = checkNotNull(tableSelector);
    }

    @Override
    public void export(NodeCreator node, Context context)
    {
        export(node, getProgressMonitor(context), getConnectionProvider(context));
    }

    private void export(final NodeCreator node, final ProgressMonitor progressMonitor, final ConnectionProvider connectionProvider)
    {
        withConnection(connectionProvider, new JdbcUtils.JdbcCallable<Void>()
        {
            public Void call(Connection connection)
            {
                try
                {
                    final Set<String> allTables = getTableNames(connection);
                    final DataExporterMonitor monitor = new DataExporterMonitor(progressMonitor, allTables.size());
                    final ConstraintsChecker constraintsChecker = new ConstraintsChecker(progressMonitor, connection.getMetaData().getDatabaseProductName());

                    monitor.start();
                    for (String table : allTables)
                    {
                        exportTable(table, connection, node, monitor, constraintsChecker);
                    }
                    node.closeEntity(); // TODO weird ??

                    monitor.end();
                    constraintsChecker.check();
                }
                catch (SQLException e)
                {
                    throw new SqlRuntimeException(e);
                }
                return null;
            }
        });
    }

    /** Returns the names of the tables that must be included in the export. */
    private Set<String> getTableNames(Connection connection) throws SQLException
    {
        Set<String> tables = new HashSet<String>();

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

    private NodeCreator exportTable(String table, Connection connection, NodeCreator node, DataExporterMonitor monitor, ConstraintsChecker constraintsChecker) throws SQLException, ParseException
    {
        monitor.startTable(table);

        TableDataNode.add(node, table);

        Statement statement = connection.createStatement();
        ResultSet result = null;
        try
        {
            statement.setFetchSize(100);
            result = statement.executeQuery("SELECT * FROM " + quote(connection, table));
            final ResultSetMetaData meta = result.getMetaData();

            // write column definitions
            node = writeColumnDefinitions(node, meta);

            while (result.next())
            {
                node = exportRow(node, result, monitor, constraintsChecker);
            }
        }
        finally
        {
            closeQuietly(result, statement);
        }

        monitor.endTable(table);
        return node.closeEntity();
    }

    private String quote(Connection connection, String table) throws SQLException
    {
        final String quoteString = connection.getMetaData().getIdentifierQuoteString().trim();
        return new StringBuilder(table.length() + 2 * quoteString.length())
                .append(quoteString).append(table).append(quoteString).toString();
    }

    private NodeCreator exportRow(NodeCreator node, ResultSet result, DataExporterMonitor monitor, ConstraintsChecker constraintsChecker) throws ParseException, SQLException
    {
        final ResultSetMetaData metaData = result.getMetaData();
        final int columns = metaData.getColumnCount();

        RowDataNode.add(node);

        for (int col = 1; col <= columns; col++)
        {
            switch (metaData.getColumnType(col))
            {
                case Types.BIGINT:
                case Types.INTEGER:
                case Types.NUMERIC:
                    RowDataNode.append(node, result.getBigDecimal(col));
                    break;

                case Types.VARCHAR:
                case Types.LONGVARCHAR:
                    final String value = result.getString(col);
                    constraintsChecker.checkVarchar(value, metaData.getColumnDisplaySize(col), metaData.getTableName(col), metaData.getColumnName(col), result.getRow());
                    RowDataNode.append(node, value);
                    break;

                case Types.BOOLEAN:
                case Types.BIT:
                    boolean bool = result.getBoolean(col);
                    RowDataNode.append(node, result.wasNull() ? null : bool);
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

        monitor.row();
        return node.closeEntity();
    }

    private NodeCreator writeColumnDefinitions(NodeCreator node, ResultSetMetaData metaData) throws SQLException, ParseException
    {
        for (int i = 1; i <= metaData.getColumnCount(); i++)
        {
            ColumnDataNode.add(node, metaData.getColumnName(i)).closeEntity();
        }
        return node;
    }

    private static final class DataExporterMonitor
    {
        private final ProgressMonitor monitor;
        private final int totalTables;

        private String currentTable = "<none>";
        private final Map<String, Integer> exportedRows;

        public DataExporterMonitor(ProgressMonitor monitor, int totalTables)
        {
            this.monitor = checkNotNull(monitor);
            this.totalTables = totalTables;
            this.exportedRows = newHashMap();
        }

        public void start()
        {
            monitor.update(Update.from("Starting export of data for %s tables", totalTables));
        }

        public void end()
        {
            monitor.update(Update.from("Exported data for %s tables", totalTables));
        }

        public void startTable(String tableName)
        {
            monitor.update(Update.from("Starting data export for table %s", tableName));
            exportedRows.put(tableName, 0);
            currentTable = tableName;
        }

        public void endTable(String tableName)
        {
            monitor.update(Update.from("Exported %s rows for table %s", exportedRows.get(tableName), tableName));
        }

        public void row()
        {
            exportedRows.put(currentTable, exportedRows.get(currentTable) + 1);
        }
    }

    private static final class ConstraintsChecker
    {
        private final ProgressMonitor monitor;
        private final String databaseProductName;
        private long constraintViolations = 0L;

        public ConstraintsChecker(ProgressMonitor monitor, String databaseProductName)
        {
            this.monitor = checkNotNull(monitor);
            this.databaseProductName = checkNotNull(databaseProductName);
        }

        public void checkVarchar(String value, double columnDisplaySize, String tableName, String columnName, int row)
        {
            if (value != null && value.length() > columnDisplaySize)
            {
                monitor.update(Update.from(
                        "Warning: %s:%d (table:row), value for column %s (%d chars) exceeds max length (%d chars).",
                        tableName, row, columnName, value.length(), columnDisplaySize));
                constraintViolations++;
            }
        }

        public void check()
        {
            if (constraintViolations > 0L)
            {
                monitor.update(Warning.from("Warning: %d database " +
                        "records had constraint violations. This backup may not " +
                        "work if you migrate to a database product other than %s.",
                        constraintViolations, databaseProductName));
            }
        }
    }
}
