package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.ImportExportErrorService;
import net.java.ao.DatabaseProvider;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.DDLForeignKey;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;

final class ActiveObjectsForeignKeyCreator implements ForeignKeyCreator
{
    private final ImportExportErrorService errorService;
    private final DatabaseProvider provider;

    public ActiveObjectsForeignKeyCreator(ImportExportErrorService errorService, DatabaseProvider provider)
    {
        this.errorService = checkNotNull(errorService);
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
                final String[] sqlStatements = provider.renderAction(a);
                for (String sql : sqlStatements)
                {
                    try
                    {
                        stmt.executeUpdate(sql);
                    }
                    catch (SQLException e)
                    {
                        throw errorService.newImportExportSqlException(a.getTable().getName(),
                                "Error creating foreign key constraint, using SQL statement '" + sql + "'", e);
                    }
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
