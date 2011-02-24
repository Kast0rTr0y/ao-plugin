package com.atlassian.dbexporter.jdbc;

import com.atlassian.dbexporter.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Convenience methods for dealing with JDBC resources.
 *
 * @author Erik van Zijst
 */
public final class JdbcUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUtils.class);

    public static <T> T withConnection(ConnectionProvider provider, JdbcCallable<T> callable)
    {
        Connection connection = null;
        try
        {
            connection = provider.getConnection();
            return callable.call(new DelegatingConnection(connection)
            {
                @Override
                public void close() throws SQLException
                {
                    // do nothing
                }
            });
        }
        catch (SQLException e)
        {
            throw new SqlRuntimeException(e);
        }
        finally
        {
            closeQuietly(connection);
        }
    }

    /**
     * Closes the specified {@link java.sql.ResultSet}, swallowing {@link java.sql.SQLException}s.
     *
     * @param resultSet
     */
    public static void closeQuietly(ResultSet resultSet)
    {
        if (resultSet != null)
        {
            try
            {
                resultSet.close();
            }
            catch (SQLException se)
            {
                LOGGER.warn("ResultSet close threw exception", se);
            }
        }
    }

    /**
     * Closes the specified {@link java.sql.Statement}, swallowing {@link SQLException}s.
     *
     * @param statement
     */
    public static void closeQuietly(Statement statement)
    {
        if (statement != null)
        {
            try
            {
                statement.close();
            }
            catch (SQLException se)
            {
                LOGGER.warn("Statement close threw exception", se);
            }
        }
    }

    /**
     * Closes the specified {@link java.sql.Connection}, swallowing {@link SQLException}s.
     *
     * @param connection
     */
    public static void closeQuietly(Connection connection)
    {
        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (SQLException se)
            {
                LOGGER.warn("Connection close threw exception", se);
            }
        }
    }

    /**
     * Closes the specified {@link ResultSet} and {@link Statement}, swallowing
     * {@link SQLException}s.
     *
     * @param resultSet
     * @param statement
     */
    public static void closeQuietly(ResultSet resultSet, Statement statement)
    {
        closeQuietly(resultSet);
        closeQuietly(statement);
    }

    public static interface JdbcCallable<T>
    {
        T call(Connection connection);
    }
}
