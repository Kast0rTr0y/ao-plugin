package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.ConnectionProvider;
import com.atlassian.dbexporter.jdbc.ImportExportSqlException;
import com.google.common.collect.ImmutableMap;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
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

            connection = getConnection();
            final DatabaseMetaData metaData = metadata(connection);

            mapBuilder.put("database.name", getDatabaseName(metaData));
            mapBuilder.put("database.version", getDatabaseVersion(metaData));
            mapBuilder.put("database.minorVersion", getDatabaseMinorVersion(metaData));
            mapBuilder.put("database.majorVersion", getDatabaseMajorVersion(metaData));

            mapBuilder.put("driver.name", getDriverName(metaData));
            mapBuilder.put("driver.version", getDriverVersion(metaData));

            return mapBuilder.build();
        }
        finally
        {
            closeQuietly(connection);
        }
    }

    private Connection getConnection()
    {
        try
        {
            return connectionProvider.getConnection();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get connection from provider", e);
        }
    }

    private static String getDatabaseName(DatabaseMetaData metaData)
    {
        try
        {
            return metaData.getDatabaseProductName();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get database product name from metadata", e);
        }
    }

    private static String getDatabaseVersion(DatabaseMetaData metaData)
    {
        try
        {
            return metaData.getDatabaseProductVersion();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get database product version from metadata", e);
        }
    }

    private static String getDatabaseMinorVersion(DatabaseMetaData metaData)
    {
        try
        {
            return String.valueOf(metaData.getDatabaseMinorVersion());
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get database minor version from metadata", e);
        }
    }

    private static String getDatabaseMajorVersion(DatabaseMetaData metaData)
    {
        try
        {
            return String.valueOf(metaData.getDatabaseMajorVersion());
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get database major version from metadata", e);
        }
    }

    private static String getDriverName(DatabaseMetaData metaData)
    {
        try
        {
            return metaData.getDriverName();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get driver name from metadata", e);
        }
    }

    private static String getDriverVersion(DatabaseMetaData metaData)
    {
        try
        {
            return metaData.getDriverVersion();
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("Could not get driver version from metadata", e);
        }
    }
}
