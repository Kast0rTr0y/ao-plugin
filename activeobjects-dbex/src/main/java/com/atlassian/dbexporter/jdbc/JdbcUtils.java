package com.atlassian.dbexporter.jdbc;

import com.atlassian.dbexporter.ConnectionProvider;
import com.atlassian.dbexporter.ImportExportErrorService;
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

    public static <T> T withConnection(ImportExportErrorService errorService, ConnectionProvider provider, JdbcCallable<T> callable)
    {
        Connection connection = null;
        try
        {
            connection = provider.getConnection();
            return callable.call(ConnectionHandler.newInstance(connection, new ConnectionHandler.Closeable()
            {
                @Override
                public void close() throws SQLException
                {
                    // do nothing
                }
            }));
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "", e);
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
     * @param errorService
     * @param table
     * @param connection the current connection being used
     * @param identifier the database identifier to quote   @return the quoted database identifier
     * @throws com.atlassian.dbexporter.ImportExportException if anything wrong happens getting information from the database connection.
     */
    public static String quote(ImportExportErrorService errorService, String table, Connection connection, String identifier)
    {
        final String quoteString = identifierQuoteString(errorService, table, connection).trim();
        return new StringBuilder(identifier.length() + 2 * quoteString.length())
                .append(quoteString).append(identifier).append(quoteString).toString();
    }

    private static String identifierQuoteString(ImportExportErrorService errorService, String table, Connection connection)
    {
        try
        {
            return metadata(errorService, connection).getIdentifierQuoteString();
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "", e);
        }
    }

    public static DatabaseMetaData metadata(ImportExportErrorService errorService, Connection connection)
    {
        try
        {
            return connection.getMetaData();
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "", e);
        }
    }

    public static Statement createStatement(ImportExportErrorService errorService, String table, Connection connection)
    {
        try
        {
            return connection.createStatement();
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not create statement from connection", e);
        }
    }

    public static PreparedStatement preparedStatement(ImportExportErrorService errorService, String table, Connection connection, String sql)
    {
        try
        {
            return connection.prepareStatement(sql);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "Could not create prepared statement for SQL query, [" + sql + "]", e);
        }
    }

    public static interface JdbcCallable<T>
    {
        T call(Connection connection);
    }
}
