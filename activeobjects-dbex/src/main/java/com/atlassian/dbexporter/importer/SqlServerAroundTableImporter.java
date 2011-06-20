package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.DatabaseInformations;
import com.atlassian.dbexporter.ImportExportException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.dbexporter.DatabaseInformations.database;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;

public final class SqlServerAroundTableImporter implements DataImporter.AroundTableImporter
{
    @Override
    public void before(ImportConfiguration configuration, String table, Connection connection)
    {
        setIdentityInsert(configuration, connection, table, "ON");
    }

    @Override
    public void after(ImportConfiguration configuration, String table, Connection connection)
    {
        setIdentityInsert(configuration, connection, table, "OFF");
    }

    private void setIdentityInsert(ImportConfiguration configuration, Connection connection, String table, String onOff)
    {
        if (isSqlServer(configuration))
        {
            setIdentityInsert(connection, table, onOff);
        }
    }

    private void setIdentityInsert(Connection connection, String table, String onOff)
    {
        Statement s = null;
        try
        {
            s = connection.createStatement();
            s.execute(setIdentityInsertSql(quote(connection, table), onOff));
        }
        catch (SQLException e)
        {
            throw new ImportExportException(e);
        }
        finally
        {
            closeQuietly(s);
        }
    }

    private String setIdentityInsertSql(String table, String onOff)
    {
        return String.format("SET IDENTITY_INSERT %s %s", table, onOff);
    }

    private boolean isSqlServer(ImportConfiguration configuration)
    {
        return DatabaseInformations.Database.Type.MSSQL.equals(database(configuration.getDatabaseInformation()).getType());
    }
}
