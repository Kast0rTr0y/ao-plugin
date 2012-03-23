package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;

public class DatabaseProviderNotFoundException extends ActiveObjectsPluginException
{
    private final String driverClassName;

    public DatabaseProviderNotFoundException(String driverClassName)
    {
        this.driverClassName = driverClassName;
    }

    public String getDriverClassName()
    {
        return driverClassName;
    }

    @Override
    public String getMessage()
    {
        return new StringBuilder()
                .append("Could not find database provider for data source which uses JDBC driver <")
                .append(driverClassName)
                .append(">")
                .toString();
    }
}
