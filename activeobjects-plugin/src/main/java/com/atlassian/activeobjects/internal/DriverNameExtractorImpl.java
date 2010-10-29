package com.atlassian.activeobjects.internal;

import net.java.ao.ActiveObjectsException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class DriverNameExtractorImpl implements DriverNameExtractor
{
    public String getDriverName(final DataSource dataSource)
    {
        Connection connection = null;
        try
        {
            connection = dataSource.getConnection();
            return connection.getMetaData().getDriverName();
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsException(e);
        }
        finally
        {
            closeQuietly(connection);
        }
    }

    private static void closeQuietly(Connection connection)
    {
        try
        {
            if (connection != null)
            {
                connection.close();
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
