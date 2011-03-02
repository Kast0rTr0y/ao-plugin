package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.ConnectionProvider;
import com.atlassian.dbexporter.jdbc.SqlRuntimeException;
import com.google.common.collect.ImmutableMap;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.closeQuietly;
import static com.google.common.base.Preconditions.*;

public final class ConnectionProviderInformationReader implements DatabaseInformationReader
{
    private final ConnectionProvider connectionProvider;

    public ConnectionProviderInformationReader(ConnectionProvider connectionProvider)
    {
        this.connectionProvider = checkNotNull(connectionProvider);
    }

    @Override
    public Map<String, String> get()
    {
        Connection connection = null;
        try
        {
            final ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();

            connection = connectionProvider.getConnection();
            final DatabaseMetaData metaData = connection.getMetaData();

            mapBuilder.put("database.name", metaData.getDatabaseProductName());
            mapBuilder.put("database.version", metaData.getDatabaseProductVersion());
            mapBuilder.put("database.minorVersion", String.valueOf(metaData.getDatabaseMinorVersion()));
            mapBuilder.put("database.majorVersion", String.valueOf(metaData.getDatabaseMajorVersion()));

            mapBuilder.put("driver.name", metaData.getDriverName());
            mapBuilder.put("driver.version", metaData.getDriverVersion());

            return mapBuilder.build();
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
}
