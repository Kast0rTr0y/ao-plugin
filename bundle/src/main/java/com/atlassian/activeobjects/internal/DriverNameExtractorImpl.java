package com.atlassian.activeobjects.internal;

import net.java.ao.ActiveObjectsException;

import javax.sql.DataSource;
import java.sql.SQLException;

public final class DriverNameExtractorImpl implements DriverNameExtractor
{
    public String getDriverName(final DataSource dataSource)
    {
        try
        {
            return dataSource.getConnection().getMetaData().getDriverName();
        }
        catch (SQLException e)
        {
            throw new ActiveObjectsException(e);
        }
    }
}
