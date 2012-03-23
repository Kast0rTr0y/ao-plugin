package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.DatabaseInformations;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.Table;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.dbexporter.DatabaseInformations.*;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;

public final class SqlServerAroundTableImporter implements DataImporter.AroundTableImporter
{
    private final ImportExportErrorService errorService;
    private final String schema;

    public SqlServerAroundTableImporter(ImportExportErrorService errorService, String schema)
    {
        this.errorService = checkNotNull(errorService);
        this.schema = schema;
    }

    @Override
    public void before(ImportConfiguration configuration, Context context, String table, Connection connection)
    {
        setIdentityInsert(configuration, context, connection, table, "ON");
    }

    @Override
    public void after(ImportConfiguration configuration, Context context, String table, Connection connection)
    {
        setIdentityInsert(configuration, context, connection, table, "OFF");
    }

    private void setIdentityInsert(ImportConfiguration configuration, Context context, Connection connection, String table, String onOff)
    {
        if (isSqlServer(configuration) && isAutoIncrementTable(context, table))
        {
            setIdentityInsert(connection, table, onOff);
        }
    }

    private boolean isAutoIncrementTable(Context context, final String tableName)
    {
        return hasAnyAutoIncrementColumn(findTable(context, tableName));
    }

    private boolean hasAnyAutoIncrementColumn(Table table)
    {
        return Iterables.any(table.getColumns(), new Predicate<Column>()
        {
            @Override
            public boolean apply(Column c)
            {
                return c.isAutoIncrement();
            }
        });
    }

    private Table findTable(Context context, final String tableName)
    {
        return Iterables.find(context.getAll(Table.class), new Predicate<Table>()
        {
            @Override
            public boolean apply(Table t)
            {
                return t.getName().equals(tableName);
            }
        });
    }

    private void setIdentityInsert(Connection connection, String table, String onOff)
    {
        Statement s = null;
        try
        {
            s = connection.createStatement();
            s.execute(setIdentityInsertSql(quote(errorService, table, connection, table), onOff));
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(table, "", e);
        }
        finally
        {
            closeQuietly(s);
        }
    }

    private String setIdentityInsertSql(String table, String onOff)
    {
        return String.format("SET IDENTITY_INSERT %s %s", schema != null ? schema + "." + table : table, onOff);
    }

    private boolean isSqlServer(ImportConfiguration configuration)
    {
        return DatabaseInformations.Database.Type.MSSQL.equals(database(configuration.getDatabaseInformation()).getType());
    }
}
