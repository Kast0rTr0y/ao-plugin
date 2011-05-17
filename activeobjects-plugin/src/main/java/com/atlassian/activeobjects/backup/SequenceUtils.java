package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.jdbc.ImportExportSqlException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.closeQuietly;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.*;

final class SequenceUtils
{
    static Iterable<TableColumnPair> tableColumnPairs(Iterable<Table> tables)
    {
        return concat(transform(tables, new AutoIncrementColumnIterableFunction()));
    }

    static void executeUpdate(Statement s, String sql)
    {
        try
        {
            s.executeUpdate(sql);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Error executing update for SQL statement '" + sql + "'", e);
        }
    }

    static void executeUpdate(Connection connection, String sql)
    {
        Statement stmt = null;
        try
        {
            stmt = connection.createStatement();
            executeUpdate(stmt, sql);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException(e);
        }
        finally
        {
            closeQuietly(stmt);
        }
    }

    static int getIntFromResultSet(ResultSet res)
    {
        try
        {
            return res.next() ? res.getInt(1) : 1;
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Error getting int value from result set.", e);
        }
    }

    static ResultSet executeQuery(Statement s, String sql)
    {
        try
        {
            return s.executeQuery(sql);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Error executing query for SQL statement '" + sql + "'", e);
        }
    }

    static class TableColumnPair
    {
        final Table table;
        final Column column;

        public TableColumnPair(Table table, Column column)
        {
            this.table = checkNotNull(table);
            this.column = checkNotNull(column);
        }
    }

    private static class AutoIncrementColumnIterableFunction implements Function<Table, Iterable<TableColumnPair>>
    {
        @Override
        public Iterable<TableColumnPair> apply(final Table table)
        {
            return transform(filter(table.getColumns(), new IsAutoIncrementColumn()), new Function<Column, TableColumnPair>()
            {
                @Override
                public TableColumnPair apply(Column column)
                {
                    return new TableColumnPair(table, column);
                }
            });
        }
    }

    private static class IsAutoIncrementColumn implements Predicate<Column>
    {
        @Override
        public boolean apply(Column column)
        {
            return column.isAutoIncrement();
        }
    }
}
