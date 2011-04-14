package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.jdbc.ImportExportSqlException;
import net.java.ao.DatabaseProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;

final class ActiveObjectsSequenceUpdater implements SequenceUpdater
{
    private final DatabaseProvider provider;

    public ActiveObjectsSequenceUpdater(DatabaseProvider provider)
    {
        this.provider = checkNotNull(provider);
    }

    @Override
    public void update(String tableName, String columnName)
    {
        Connection connection = null;
        Statement maxStmt = null;
        Statement alterSeqStmt = null;
        try
        {
            connection = provider.getConnection();
            maxStmt = connection.createStatement();

            final ResultSet res = executeQuery(maxStmt, maxSql(connection, tableName, columnName));

            final int max = getIntFromResultSet(res);
            alterSeqStmt = connection.createStatement();
            executeUpdate(alterSeqStmt, alterSequenceSql(connection, tableName, columnName, max + 1));
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException(e);
        }
        finally
        {
            closeQuietly(maxStmt, alterSeqStmt);
            closeQuietly(connection);
        }
    }

    private String alterSequenceSql(Connection connection, String tableName, String columnName, int val)
    {
        return "ALTER SEQUENCE " + quote(connection, sequenceName(tableName, columnName)) + " RESTART WITH " + val;
    }

    private String sequenceName(String tableName, String columnName)
    {
        return tableName + "_" + columnName + "_" + "seq";
    }

    private String maxSql(Connection connection, String tableName, String columnName)
    {
        return "SELECT MAX(" + quote(connection, columnName) + ") FROM " + quote(connection, tableName);
    }

    private static int getIntFromResultSet(ResultSet res)
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

    private static ResultSet executeQuery(Statement s, String sql)
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

    private static void executeUpdate(Statement s, String sql)
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
}
