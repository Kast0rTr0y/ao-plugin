package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.BatchMode;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.jdbc.JdbcUtils;
import com.atlassian.dbexporter.jdbc.SqlRuntimeException;
import com.atlassian.dbexporter.node.NodeParser;
import com.atlassian.dbexporter.node.ParseException;
import com.atlassian.dbexporter.progress.Update;
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

import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.atlassian.dbexporter.node.NodeBackup.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

public final class DataImporter extends AbstractSingleNodeImporter
{
    public DataImporter(List<AroundImporter> arounds)
    {
        super(arounds);
    }

    public DataImporter(AroundImporter... arounds)
    {
        this(newArrayList(checkNotNull(arounds)));
    }

    @Override
    protected String getNodeName()
    {
        return TableDataNode.NAME;
    }

    @Override
    protected void doImportNode(final NodeParser node, final ImportConfiguration configuration, final Context context)
    {
        withConnection(configuration.getConnectionProvider(), new JdbcUtils.JdbcCallable<Void>()
        {
            public Void call(Connection connection)
            {
                try
                {
                    final boolean autoCommit = connection.getAutoCommit();
                    connection.setAutoCommit(false);
                    try
                    {
                        for (; TableDataNode.NAME.equals(node.getName()) && !node.isClosed(); node.getNextNode())
                        {
                            importTable(node, configuration, connection);
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
                    throw new SqlRuntimeException(e);
                }
                return null;
            }
        });
    }

    private NodeParser importTable(NodeParser node, ImportConfiguration configuration, Connection connection) throws ParseException, SQLException
    {
        final EntityNameProcessor entityNameProcessor = configuration.getEntityNameProcessor();

        long rowNum = 0L;

        final String currentTable = entityNameProcessor.tableName(TableDataNode.getName(node));
        final InserterBuilder builder = new InserterBuilder(currentTable, configuration.getBatchMode());

        node = node.getNextNode();
        for (; ColumnDataNode.NAME.equals(node.getName()) && !node.isClosed(); node = node.getNextNode())
        {
            final String column = ColumnDataNode.getName(node);
            builder.addColumn(entityNameProcessor.columnName(column));
            node = node.getNextNode();  // close column node
        }

        final Inserter inserter = builder.build(connection);
        try
        {
            try
            {
                for (; RowDataNode.NAME.equals(node.getName()) && !node.isClosed(); node = node.getNextNode())
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
            finally
            {
                inserter.close();
            }
        }
        catch (SQLException se)
        {
            configuration.getProgressMonitor().update(Update.from("Database error at %s:%d (table:row) of the input: %s", currentTable, rowNum, se.getMessage()));
            throw se;
        }
        return node;
    }

    private static interface Inserter
    {
        void setValue(NodeParser node) throws SQLException, ParseException;

        void execute() throws SQLException;

        void close() throws SQLException;
    }


    private static class InserterBuilder
    {
        private final String table;
        private final BatchMode batch;
        private final List<String> columns;

        public InserterBuilder(String table, BatchMode batch)
        {
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

        public Inserter build(Connection connection) throws SQLException
        {

            final StringBuilder query = new StringBuilder("INSERT INTO ")
                    .append(quote(connection, table))
                    .append(" (");

            for (int i = 0; i < columns.size(); i++)
            {
                query.append(quote(connection, columns.get(i)));
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
            List<Integer> maxColumnSizes = calculateColumnSizes(connection, columns);

            final PreparedStatement ps = connection.prepareStatement(query.toString());
            return newInserter(maxColumnSizes, ps);
        }

        private Inserter newInserter(List<Integer> maxColumnSizes, PreparedStatement ps)
        {
            return batch.equals(BatchMode.ON) ?
                    new BatchInserter(getTable(), columns, ps, maxColumnSizes) :
                    new ImmediateInserter(getTable(), columns, ps, maxColumnSizes);
        }

        /** Get the column size for all columns in the table -- only the sizes for String columns will be used */
        private List<Integer> calculateColumnSizes(Connection connection, List<String> columns) throws SQLException
        {
            Map<String, Integer> columnSizeMap = Maps.newHashMap();
            ResultSet rs = null;
            try
            {
                rs = connection.getMetaData().getColumns(null, null, table, null);
                while (rs.next())
                {
                    String columnName = rs.getString("COLUMN_NAME");
                    int columnSize = rs.getInt("COLUMN_SIZE");
                    columnSizeMap.put(columnName, columnSize);
                }
                final List<Integer> sizes = newArrayList();
                sizes.add(0); // dummy, column indices start at 1
                for (String column : columns)
                {
                    Integer size = columnSizeMap.get(column);
                    if (size == null)
                    {
                        // if we don't have a size, assume no limit
                        size = -1;
                    }
                    sizes.add(size);
                }
                return sizes;
            }
            finally
            {
                closeQuietly(rs);
            }
        }
    }

    private static abstract class BaseInserter implements Inserter
    {
        private int col;
        private final String tableName;
        // this list is zero based
        private final List<String> columnNames;
        protected final PreparedStatement ps;
        // indices into this list are 1 based -- values of -1 indicate that we don't know the max length and assume there is no limit
        // e.g. HSQL doesn't provide sizes
        private final List<Integer> maxColumnSize;

        public BaseInserter(String tableName, List<String> columnNames, PreparedStatement ps, List<Integer> maxColumnSize)
        {
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
                    String oldValue = value;
                    value = oldValue.substring(0, maxSize);
                    String message = "Truncating value of column " + getTableName() + "." + getColumnName(col) +
                            " from '" + oldValue + "' to '" + value + "' because its length of "
                            + oldValue.length() + " is greater than the maximum allowed length for this column of " + maxSize + ".";
//                        logger.warning(message);
//                        monitor.(new Update(message)); TODO
                }
                ps.setString(col, value);
            }
        }

        private String getColumnName(int col)
        {
            return columnNames.get(col - 1);
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

        public void setValue(NodeParser node) throws SQLException, ParseException
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

        public String getTableName()
        {
            return tableName;
        }
    }

    private static class ImmediateInserter extends BaseInserter
    {
        private ImmediateInserter(String table, List<String> columns, PreparedStatement ps, List<Integer> maxColumnSize)
        {
            super(table, columns, ps, maxColumnSize);
        }

        protected void executePS() throws SQLException
        {
            ps.execute();
        }

        public void close() throws SQLException
        {
            closeQuietly(ps);
        }
    }

    private static class BatchInserter extends BaseInserter
    {
        private final int batchSize;
        private int batch;

        private BatchInserter(String table, List<String> columns, PreparedStatement ps, List<Integer> maxColumnSize)
        {
            super(table, columns, ps, maxColumnSize);
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

        private void flush() throws SQLException
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

        public void close() throws SQLException
        {
            flush();
            JdbcUtils.closeQuietly(ps);
        }
    }
}
