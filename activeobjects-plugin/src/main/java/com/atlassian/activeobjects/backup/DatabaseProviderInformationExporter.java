package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.exporter.DatabaseInformationReader;
import com.atlassian.dbexporter.jdbc.SqlRuntimeException;
import com.google.common.collect.ImmutableMap;
import net.java.ao.DatabaseProvider;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.closeQuietly;
import static com.google.common.base.Preconditions.*;

final class DatabaseProviderInformationExporter implements DatabaseInformationReader
{
    private final DatabaseProvider databaseProvider;

    public DatabaseProviderInformationExporter(DatabaseProvider databaseProvider)
    {
        this.databaseProvider = checkNotNull(databaseProvider);
    }

    @Override
    public Map<String, String> get()
    {
        Connection connection = null;
        try
        {
            final ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();

            connection = databaseProvider.getConnection();
            final DatabaseMetaData metaData = connection.getMetaData();

            mapBuilder.put("database.name", metaData.getDatabaseProductName());
            mapBuilder.put("database.version", metaData.getDatabaseProductVersion());
            mapBuilder.put("database.minorVersion", String.valueOf(metaData.getDatabaseMinorVersion()));
            mapBuilder.put("database.majorVersion", String.valueOf(metaData.getDatabaseMajorVersion()));

            mapBuilder.put("driver.name", metaData.getDriverName());
            mapBuilder.put("driver.version", metaData.getDriverVersion());

            return mapBuilder.build();  //To change body of implemented methods use File | Settings | File Templates.
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
