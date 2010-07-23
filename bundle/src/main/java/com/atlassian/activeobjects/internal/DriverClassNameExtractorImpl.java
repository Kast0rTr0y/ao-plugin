package com.atlassian.activeobjects.internal;

import net.java.ao.ActiveObjectsException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class DriverClassNameExtractorImpl implements DriverClassNameExtractor
{
    public String getDriverClassName(DataSource dataSource)
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

    private void closeQuietly(Connection connection)
    {
        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (SQLException e)
            {
                // ignored
            }
        }
    }
}
