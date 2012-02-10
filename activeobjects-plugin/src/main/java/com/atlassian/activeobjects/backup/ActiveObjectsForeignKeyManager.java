package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.importer.ForeignKeyManager;
import com.google.common.collect.Lists;

import net.java.ao.DatabaseProvider;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.SQLAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import static com.atlassian.activeobjects.backup.SqlUtils.*;
import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsForeignKeyManager implements ForeignKeyManager
{
    private final ImportExportErrorService errorService;
    private final NameConverters converters;
    private final DatabaseProvider provider;

    public ActiveObjectsForeignKeyManager(ImportExportErrorService errorService, DatabaseProvider provider, NameConverters converters)
    {
        this.errorService = checkNotNull(errorService);
        this.converters = checkNotNull(converters);
        this.provider = checkNotNull(provider);
    }
    
    public void create(Iterable<ForeignKey> foreignKeys, EntityNameProcessor entityNameProcessor)
    {
        alter(DDLActionType.ALTER_ADD_KEY, foreignKeys, entityNameProcessor);
    }
    
    private void drop(Iterable<ForeignKey> foreignKeys, EntityNameProcessor entityNameProcessor)
    {
        alter(DDLActionType.ALTER_DROP_KEY, foreignKeys, entityNameProcessor);
    }
    
    public Iterable<Table> dropForTables(Iterable<Table> tables, EntityNameProcessor entityNameProcessor)
    {
        List<ForeignKey> foreignKeys = Lists.newArrayList();
        List<Table> tablesWithoutForeignKeys = Lists.newArrayList();
        for (Table table : tables)
        {
            foreignKeys.addAll(table.getForeignKeys());
            tablesWithoutForeignKeys.add(new Table(table.getName(), table.getColumns(), Collections.<ForeignKey>emptyList()));
        }
        drop(foreignKeys, entityNameProcessor);
        return tablesWithoutForeignKeys;
    }

    private void alter(DDLActionType ddlActionType, Iterable<ForeignKey> foreignKeys, EntityNameProcessor entityNameProcessor)
    {
        Connection conn = null;
        Statement stmt = null;
        try
        {
            conn = provider.getConnection();
            stmt = conn.createStatement();
            for (ForeignKey foreignKey : foreignKeys)
            {
                final DDLAction a = new DDLAction(ddlActionType);
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

    private static String tableName(DDLAction a)
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

    private static DDLForeignKey toDdlForeignKey(ForeignKey foreignKey, EntityNameProcessor entityNameProcessor)
    {
        final DDLForeignKey ddlForeignKey = new DDLForeignKey();
        ddlForeignKey.setDomesticTable(entityNameProcessor.tableName(foreignKey.getFromTable()));
        ddlForeignKey.setField(entityNameProcessor.columnName(foreignKey.getFromField()));
        ddlForeignKey.setTable(entityNameProcessor.tableName(foreignKey.getToTable()));
        ddlForeignKey.setForeignField(entityNameProcessor.columnName(foreignKey.getToField()));
        return ddlForeignKey;
    }
}
