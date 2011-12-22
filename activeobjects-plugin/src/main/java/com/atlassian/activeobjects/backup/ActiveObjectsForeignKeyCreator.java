package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.ImportExportErrorService;
import net.java.ao.DatabaseProvider;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.SQLAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.activeobjects.backup.SqlUtils.*;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;

final class ActiveObjectsForeignKeyCreator implements ForeignKeyCreator
{
    private final ImportExportErrorService errorService;
    private final NameConverters converters;
    private final DatabaseProvider provider;

    public ActiveObjectsForeignKeyCreator(ImportExportErrorService errorService, NameConverters converters, DatabaseProvider provider)
    {
        this.errorService = checkNotNull(errorService);
        this.converters = checkNotNull(converters);
        this.provider = checkNotNull(provider);
    }

    public void create(Iterable<ForeignKey> foreignKeys, EntityNameProcessor entityNameProcessor)
    {
        Connection conn = null;
        Statement stmt = null;
        try
        {
            conn = provider.getConnection();
            stmt = conn.createStatement();
            for (ForeignKey foreignKey : foreignKeys)
            {
                final DDLAction a = new DDLAction(DDLActionType.ALTER_ADD_KEY);
                a.setKey(toDdlForeignKey(foreignKey, entityNameProcessor));
                final Iterable<SQLAction> sqlActions = provider.renderAction(converters, a);
                for (SQLAction sql : sqlActions)
                {
                    executeUpdate(errorService, tableName(a), stmt, sql.getStatement());
                }
            }
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "", e);
        }
        finally
        {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    private String tableName(DDLAction a)
    {
        if (a == null)
        {
            return null;
        }
        if (a.getTable() == null)
        {
            return null;
        }
        return a.getTable().getName();
    }

    private DDLForeignKey toDdlForeignKey(ForeignKey foreignKey, EntityNameProcessor entityNameProcessor)
    {
        final DDLForeignKey ddlForeignKey = new DDLForeignKey();
        ddlForeignKey.setDomesticTable(entityNameProcessor.tableName(foreignKey.getFromTable()));
        ddlForeignKey.setField(entityNameProcessor.columnName(foreignKey.getFromField()));
        ddlForeignKey.setTable(entityNameProcessor.tableName(foreignKey.getToTable()));
        ddlForeignKey.setForeignField(entityNameProcessor.columnName(foreignKey.getToField()));
        return ddlForeignKey;
    }
}
