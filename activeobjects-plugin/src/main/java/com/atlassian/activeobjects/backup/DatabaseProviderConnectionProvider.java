package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.ConnectionProvider;
import net.java.ao.DatabaseProvider;

import java.sql.Connection;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkNotNull;

final class DatabaseProviderConnectionProvider implements ConnectionProvider
{
    private final DatabaseProvider provider;

    public DatabaseProviderConnectionProvider(DatabaseProvider provider)
    {
        this.provider = checkNotNull(provider);
    }

    public Connection getConnection() throws SQLException
    {
        return provider.getConnection();
    }
}
