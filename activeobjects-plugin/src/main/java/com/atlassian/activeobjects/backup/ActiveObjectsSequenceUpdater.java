package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.jdbc.SqlRuntimeException;
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

            final ResultSet res = maxStmt.executeQuery(maxSql(connection, tableName, columnName));
            final int max = res.next() ? res.getInt(1) : 1;

            alterSeqStmt = connection.createStatement();
            alterSeqStmt.executeUpdate(alterSequenceSql(connection, tableName, columnName, max + 1));
        }
        catch (SQLException e)
        {
            throw new SqlRuntimeException(e);
        }
        finally
        {
            closeQuietly(maxStmt, alterSeqStmt);
            closeQuietly(connection);
        }
    }

    private String alterSequenceSql(Connection connection, String tableName, String columnName, int val) throws SQLException
    {
        return "ALTER SEQUENCE " + quote(connection, sequenceName(tableName, columnName)) + " RESTART WITH " + val;
    }

    private String sequenceName(String tableName, String columnName)
    {
        return tableName + "_" + columnName + "_" + "seq";
    }

    private String maxSql(Connection connection, String tableName, String columnName) throws SQLException
    {
        return "SELECT MAX(" + quote(connection, columnName) + ") FROM " + quote(connection, tableName);
    }
}
