package com.atlassian.dbexporter.jdbc;

import com.atlassian.dbexporter.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
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
            throw new ImportExportSqlException(e);
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
     * @param statements the list of statements to close
     */
    public static void closeQuietly(Statement... statements)
    {
        for (Statement statement : statements)
        {
            closeQuietly(statement);
        }
    }

    private static void closeQuietly(Statement statement)
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

    /**
     * Quotes the database identifier if needed.
     *
     * @param connection the current connection being used
     * @param identifier the database identifier to quote
     * @return the quoted database identifier
     * @throws ImportExportSqlException if anything wrong happens getting information from the database connection.
     */
    public static String quote(Connection connection, String identifier)
    {
        final String quoteString = identifierQuoteString(connection).trim();
        return new StringBuilder(identifier.length() + 2 * quoteString.length())
                .append(quoteString).append(identifier).append(quoteString).toString();
    }

    private static String identifierQuoteString(Connection connection)
    {
        try
        {
            return metadata(connection).getIdentifierQuoteString();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException(e);
        }
    }

    public static DatabaseMetaData metadata(Connection connection)
    {
        try
        {
            return connection.getMetaData();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException(e);
        }
    }

    public static Statement createStatement(Connection connection)
    {
        try
        {
            return connection.createStatement();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not create statement from connection", e);
        }
    }

    public static PreparedStatement preparedStatement(Connection connection, String sql)
    {
        try
        {
            return connection.prepareStatement(sql);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not create prepared statement for SQL query, [" + sql + "]", e);
        }
    }

    public static interface JdbcCallable<T>
    {
        T call(Connection connection);
    }
}
