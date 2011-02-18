package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.jdbc.SqlRuntimeException;
import net.java.ao.DatabaseProvider;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.DDLForeignKey;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.closeQuietly;
import static com.google.common.base.Preconditions.checkNotNull;

final class ActiveObjectsForeignKeyCreator implements ForeignKeyCreator
{
    private final DatabaseProvider provider;

    public ActiveObjectsForeignKeyCreator(DatabaseProvider provider)
    {
        this.provider = checkNotNull(provider);
    }

    public void create(Iterable<ForeignKey> foreignKeys, Context context)
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
                a.setKey(toDdlForeignKey(foreignKey));
                final String[] sqlStatements = provider.renderAction(a);
                for (String sql : sqlStatements)
                {
                    try
                    {
                        stmt.executeUpdate(sql);
                    }
                    catch (SQLException e)
                    {
                        throw new SqlRuntimeException(e);
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new SqlRuntimeException(e);
        }
        finally
        {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    private DDLForeignKey toDdlForeignKey(ForeignKey foreignKey)
    {
        final DDLForeignKey ddlForeignKey = new DDLForeignKey();
        ddlForeignKey.setDomesticTable(foreignKey.getFromTable());
        ddlForeignKey.setField(foreignKey.getFromField());
        ddlForeignKey.setTable(foreignKey.getToTable());
        ddlForeignKey.setForeignField(foreignKey.getToField());
        return ddlForeignKey;
    }
}