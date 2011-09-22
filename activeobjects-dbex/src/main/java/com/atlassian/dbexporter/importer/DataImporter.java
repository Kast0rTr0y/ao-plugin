package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.BatchMode;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.jdbc.JdbcUtils;
import com.atlassian.dbexporter.node.NodeParser;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import com.google.common.collect.Maps;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.atlassian.dbexporter.importer.ImporterUtils.*;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.atlassian.dbexporter.progress.ProgressMonitor.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

public final class DataImporter extends AbstractSingleNodeImporter
{
    private final String schema;
    private final AroundTableImporter aroundTable;

    public DataImporter(ImportExportErrorService errorService, String schema, AroundTableImporter aroundTableImporter, List<AroundImporter> arounds)
    {
        super(errorService, arounds);
        this.schema = isBlank(schema) ? null : schema;
        this.aroundTable = checkNotNull(aroundTableImporter);
    }

    public DataImporter(ImportExportErrorService errorService, String schema, AroundTableImporter aroundTableImporter, AroundImporter... arounds)
    {
        this(errorService, schema, aroundTableImporter, newArrayList(checkNotNull(arounds)));
    }

    @Override
    protected String getNodeName()
    {
        return TableDataNode.NAME;
    }

    @Override
    protected void doImportNode(final NodeParser node, final ImportConfiguration configuration, final Context context)
    {
        final ProgressMonitor monitor = configuration.getProgressMonitor();
        monitor.begin(Task.TABLES_DATA);
        withConnection(errorService, configuration.getConnectionProvider(), new JdbcUtils.JdbcCallable<Void>()
        {
            public Void call(Connection connection)
            {
                try
                {
                    final boolean autoCommit = connection.getAutoCommit();
                    try
                    {
                        connection.setAutoCommit(false);
                        for (; TableDataNode.NAME.equals(node.getName()) && !node.isClosed(); node.getNextNode())
                        {
                            importTable(node, configuration, context, connection);
                        }
                        connection.commit();
                    }
                    finally
                    {
                        connection.setAutoCommit(autoCommit);   // restore autocommit
                    }
                }
                catch (SQLException e)
                {
                    throw errorService.newImportExportSqlException(null, "", e);
                }
                return null;
            }
        });
        monitor.end(Task.TABLES_DATA);
    }

    private NodeParser importTable(NodeParser node, ImportConfiguration configuration, Context context, Connection connection)
    {
        final ProgressMonitor monitor = configuration.getProgressMonitor();
        final EntityNameProcessor entityNameProcessor = configuration.getEntityNameProcessor();

        final String currentTable = entityNameProcessor.tableName(TableDataNode.getName(node));

        monitor.begin(Task.TABLE_DATA, currentTable);

        final InserterBuilder builder = new InserterBuilder(errorService, schema, currentTable, configuration.getBatchMode());

        node = node.getNextNode();
        for (; isNodeNotClosed(node, ColumnDataNode.NAME); node = node.getNextNode())
        {
            final String column = ColumnDataNode.getName(node);
            builder.addColumn(entityNameProcessor.columnName(column));
            node = node.getNextNode();  // close column node
        }

        final Inserter inserter = builder.build(connection);

        long rowNum = 0L;
        try
        {
            aroundTable.before(configuration, context, currentTable, connection);

            for (; isNodeNotClosed(node, RowDataNode.NAME); node = node.getNextNode())
            {
                node = node.getNextNode();  // read the first field node
                for (; !node.isClosed(); node = node.getNextNode())
                {
                    inserter.setValue(node);
                }
                inserter.execute();
                rowNum++;
            }
        }
        catch (SQLException e)
        {
            throw errorService.newRowImportSqlException(currentTable, rowNum, e);
        }
        finally
        {
            inserter.close();
            aroundTable.after(configuration, context, currentTable, connection);
        }

        monitor.end(Task.TABLE_DATA, currentTable);

        return node;
    }

    private static interface Inserter
    {
        void setValue(NodeParser node) throws SQLException;

        void execute() throws SQLException;

        void close();
    }


    private static class InserterBuilder
    {
        public static final int UNLIMITED_COLUMN_SIZE = -1;

        private final ImportExportErrorService errorService;
        private final String schema;
        private final String table;
        private final BatchMode batch;
        private final List<String> columns;

        public InserterBuilder(ImportExportErrorService errorService, String schema, String table, BatchMode batch)
        {
            this.errorService = checkNotNull(errorService);
            this.schema = schema;
            this.table = table;
            this.batch = batch;
            columns = new ArrayList<String>();
        }

        public String getTable()
        {
            return table;
        }

        public void addColumn(String column)
        {
            columns.add(column);
        }

        public Inserter build(Connection connection)
        {
            final StringBuilder query = new StringBuilder("INSERT INTO ")
                    .append(tableName(connection))
                    .append(" (");

            for (int i = 0; i < columns.size(); i++)
            {
                query.append(quote(errorService, table, connection, columns.get(i)));
                if (i < columns.size() - 1)
                {
                    query.append(", ");
                }
            }

            query.append(") VALUES (");
            for (int i = 0; i < columns.size(); i++)
            {
                query.append("?");
                if (i < columns.size() - 1)
                {
                    query.append(", ");
                }
            }
            query.append(")");

            final List<Integer> maxColumnSizes = calculateColumnSizes(connection, columns);
            final PreparedStatement ps = preparedStatement(errorService, table, connection, query.toString());
            return newInserter(maxColumnSizes, ps);
        }

        private String tableName(Connection connection)
        {
            final String quoted = quote(errorService, table, connection, table);
            return schema != null ? schema + "." + quoted : quoted;
        }

        private Inserter newInserter(List<Integer> maxColumnSizes, PreparedStatement ps)
        {
            return batch.equals(BatchMode.ON) ?
                    new BatchInserter(errorService, getTable(), columns, ps, maxColumnSizes) :
                    new ImmediateInserter(errorService, getTable(), columns, ps, maxColumnSizes);
        }

        /**
         * Get the column size for all columns in the table -- only the sizes for String columns will be used
         *
         * @param connection
         * @param columns
         * @return
         */
        private List<Integer> calculateColumnSizes(Connection connection, List<String> columns)
        {
            Map<String, Integer> columnSizeMap = Maps.newHashMap();
            ResultSet rs = null;
            try
            {
                rs = getColumnsResultSet(connection);
                ColumnNameAndSize columnNameAndSize = getColumnNameAndSize(rs);
                while (columnNameAndSize != ColumnNameAndSize.NULL)
                {
                    columnSizeMap.put(columnNameAndSize.name, columnNameAndSize.size);
                    columnNameAndSize = getColumnNameAndSize(rs);
                }

                final List<Integer> sizes = newArrayList(0);
                for (String column : columns)
                {
                    final Integer size = columnSizeMap.get(column);
                    sizes.add(size != null ? size : UNLIMITED_COLUMN_SIZE);
                }
                return sizes;
            }
            finally
            {
                closeQuietly(rs);
            }
        }

        private ColumnNameAndSize getColumnNameAndSize(ResultSet rs)
        {
            try
            {
                if (rs.next())
                {
                    final String name = rs.getString("COLUMN_NAME");
                    final int size = rs.getInt("COLUMN_SIZE");
                    final int type = rs.getInt("DATA_TYPE");
                    return new ColumnNameAndSize(name, type == Types.CLOB ? UNLIMITED_COLUMN_SIZE : size);
                }
                else
                {
                    return ColumnNameAndSize.NULL;
                }
            }
            catch (SQLException e)
            {
                throw errorService.newImportExportSqlException(table, "", e);
            }
        }

        private ResultSet getColumnsResultSet(Connection connection)
        {
            try
            {
                return metadata(errorService, connection).getColumns(null, null, table, null);
            }
            catch (SQLException e)
            {
                throw errorService.newImportExportSqlException(table, "", e);
            }
        }
    }

    private static class ColumnNameAndSize
    {
        private static ColumnNameAndSize NULL = new ColumnNameAndSize();

        public final String name;
        public final int size;

        private ColumnNameAndSize()
        {
            this.name = null;
            this.size = InserterBuilder.UNLIMITED_COLUMN_SIZE;
        }

        public ColumnNameAndSize(String name, int size)
        {
            this.name = checkNotNull(name);
            this.size = size <= 0 ? InserterBuilder.UNLIMITED_COLUMN_SIZE : size;
        }
    }

    private static abstract class BaseInserter implements Inserter
    {
        protected final ImportExportErrorService errorService;
        protected final String tableName;

        private int col;
        // this list is zero based
        private final List<String> columnNames;
        protected final PreparedStatement ps;
        // indices into this list are 1 based -- values of -1 indicate that we don't know the max length and assume there is no limit
        // e.g. HSQL doesn't provide sizes
        private final List<Integer> maxColumnSize;

        public BaseInserter(ImportExportErrorService errorService, String tableName, List<String> columnNames, PreparedStatement ps, List<Integer> maxColumnSize)
        {
            this.errorService = checkNotNull(errorService);
            this.tableName = tableName;
            this.columnNames = columnNames;
            this.ps = ps;
            this.maxColumnSize = maxColumnSize;
            col = 1;
        }

        private void setBoolean(Boolean value) throws SQLException
        {
            if (value == null)
            {
                ps.setNull(col, Types.BOOLEAN);
            }
            else
            {
                ps.setBoolean(col, value);
            }
        }

        private void setString(String value) throws SQLException
        {
            if (value == null)
            {
                ps.setNull(col, Types.VARCHAR);
            }
            else
            {
                int maxSize = maxColumnSize.get(col);
                if (maxSize != -1 && value.length() > maxSize)
                {
                    throw errorService.newImportExportException(tableName, "Could not import data in table '" + tableName + "' column #" + col + ", value is too big for column which size limit is " + maxSize + ", value is:\n" + value + "\n");
                }
                ps.setString(col, value);
            }
        }

        private void setDate(Date value) throws SQLException
        {
            if (value == null)
            {
                ps.setNull(col, Types.TIMESTAMP);
            }
            else
            {
                ps.setTimestamp(col, new Timestamp(value.getTime()));
            }
        }

        private void setBigInteger(BigInteger value) throws SQLException
        {
            if (value == null)
            {
                ps.setNull(col, Types.BIGINT);
            }
            else
            {
                ps.setBigDecimal(col, new BigDecimal(value));
            }
        }

        private void setBigDecimal(BigDecimal value) throws SQLException
        {
            if (value == null)
            {
                ps.setNull(col, Types.DOUBLE);
            }
            else
            {
                ps.setBigDecimal(col, value);
            }
        }

        public void setValue(NodeParser node) throws SQLException
        {
            if (RowDataNode.isString(node))
            {
                setString(node.getContentAsString());
            }
            else if (RowDataNode.isBoolean(node))
            {
                setBoolean(node.getContentAsBoolean());
            }
            else if (RowDataNode.isInteger(node))
            {
                setBigInteger(node.getContentAsBigInteger());
            }
            else if (RowDataNode.isDouble(node))
            {
                setBigDecimal(node.getContentAsBigDecimal());
            }
            else if (RowDataNode.isDate(node))
            {
                setDate(node.getContentAsDate());
            }
            else
            {
                throw new IllegalArgumentException("Unsupported field encountered: " + node.getName());
            }
            col++;
        }

        public final void execute() throws SQLException
        {
            executePS();
            col = 1;
        }

        protected abstract void executePS() throws SQLException;
    }

    private static class ImmediateInserter extends BaseInserter
    {
        private ImmediateInserter(ImportExportErrorService errorService, String table, List<String> columns, PreparedStatement ps, List<Integer> maxColumnSize)
        {
            super(errorService, table, columns, ps, maxColumnSize);
        }

        protected void executePS() throws SQLException
        {
            ps.execute();
        }

        public void close()
        {
            closeQuietly(ps);
        }
    }

    private static class BatchInserter extends BaseInserter
    {
        private final int batchSize;
        private int batch;

        private BatchInserter(ImportExportErrorService errorService, String table, List<String> columns, PreparedStatement ps, List<Integer> maxColumnSize)
        {
            super(errorService, table, columns, ps, maxColumnSize);
            batchSize = 5000;
            batch = 0;
        }

        protected void executePS() throws SQLException
        {
            ps.addBatch();
            if ((batch = (batch + 1) % batchSize) == 0)
            {
                flush();
            }
        }

        private void flush()
        {
            try
            {
                for (int result : ps.executeBatch())
                {
                    if (result == Statement.EXECUTE_FAILED)
                    {
                        throw new SQLException("SQL batch insert failed.");
                    }
                }
                ps.getConnection().commit();
            }
            catch (SQLException e)
            {
                throw errorService.newImportExportSqlException(tableName, "", e);
            }
        }

        public void close()
        {
            flush();
            closeQuietly(ps);
        }
    }

    public static interface AroundTableImporter
    {
        void before(ImportConfiguration configuration, Context context, String table, Connection connection);

        void after(ImportConfiguration configuration, Context context, String table, Connection connection);
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
