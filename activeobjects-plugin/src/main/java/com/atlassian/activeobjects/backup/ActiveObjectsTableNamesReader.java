package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.ImportExportException;
import net.java.ao.DatabaseProvider;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.helper.DatabaseMetaDataReader;
import net.java.ao.schema.helper.DatabaseMetaDataReaderImpl;

import java.sql.Connection;
import java.sql.SQLException;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;

public class ActiveObjectsTableNamesReader
{
    public Iterable<String> getTableNames(DatabaseProvider databaseProvider, SchemaConfiguration schemaConfiguration)
    {
        Connection connection = null;
        try
        {
            connection = databaseProvider.getConnection();
            return newDatabaseMetaDataReader(databaseProvider, schemaConfiguration).getTableNames(connection.getMetaData());
        }
        catch (SQLException e)
        {
            throw new ImportExportException(e);
        }
        finally
        {
            closeQuietly(connection);
        }
    }

    private static DatabaseMetaDataReader newDatabaseMetaDataReader(DatabaseProvider databaseProvider, SchemaConfiguration schemaConfiguration)
    {
        return new DatabaseMetaDataReaderImpl(databaseProvider, schemaConfiguration);
    }
}
