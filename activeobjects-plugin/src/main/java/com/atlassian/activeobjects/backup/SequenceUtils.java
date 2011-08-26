package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.Table;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Iterables.*;

final class SequenceUtils
{
    static Iterable<TableColumnPair> tableColumnPairs(Iterable<Table> tables)
    {
        return concat(transform(tables, new AutoIncrementColumnIterableFunction()));
    }

    static void executeUpdate(ImportExportErrorService errorService, String tableName, Statement s, String sql)
    {
        try
        {
            s.executeUpdate(sql);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(tableName, "Error executing update for SQL statement '" + sql + "'", e);
        }
    }

    static void executeUpdate(ImportExportErrorService errorService, String tableName, Connection connection, String sql)
    {
        Statement stmt = null;
        try
        {
            stmt = connection.createStatement();
            executeUpdate(errorService, tableName, stmt, sql);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(tableName, "", e);
        }
        finally
        {
            closeQuietly(stmt);
        }
    }

    static int getIntFromResultSet(ImportExportErrorService errorService, String tableName, ResultSet res)
    {
        try
        {
            return res.next() ? res.getInt(1) : 1;
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(tableName, "Error getting int value from result set.", e);
        }
    }

    static ResultSet executeQuery(ImportExportErrorService errorService, String tableName, Statement s, String sql)
    {
        try
        {
            return s.executeQuery(sql);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(tableName, "Error executing query for SQL statement '" + sql + "'", e);
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
