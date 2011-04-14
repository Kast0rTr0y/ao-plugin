package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.importer.TableCreator;
import com.atlassian.dbexporter.jdbc.ImportExportSqlException;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import net.java.ao.DatabaseProvider;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.types.TypeManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import static com.atlassian.dbexporter.jdbc.JdbcUtils.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

final class ActiveObjectsTableCreator implements TableCreator
{
    private final DatabaseProvider provider;

    public ActiveObjectsTableCreator(DatabaseProvider provider)
    {
        this.provider = checkNotNull(provider);
    }

    public void create(Iterable<Table> tables, EntityNameProcessor entityNameProcessor, ProgressMonitor monitor)
    {
        Connection conn = null;
        Statement stmt = null;
        try
        {
            conn = provider.getConnection();
            stmt = conn.createStatement();

            for (Table table : tables)
            {
                monitor.begin(ProgressMonitor.Task.TABLE_CREATION, entityNameProcessor.tableName(table.getName()));
                create(stmt, table, entityNameProcessor);
                monitor.end(ProgressMonitor.Task.TABLE_CREATION, entityNameProcessor.tableName(table.getName()));
            }
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException(e);
        }
        finally
        {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    private void create(Statement stmt, Table table, EntityNameProcessor entityNameProcessor)
    {
        final DDLAction a = new DDLAction(DDLActionType.CREATE);
        a.setTable(toDdlTable(table, entityNameProcessor));
        final String[] sqlStatements = provider.renderAction(a);
        for (String sql : sqlStatements)
        {
            try
            {
                stmt.executeUpdate(sql);
            }
            catch (SQLException e)
            {
                throw new ImportExportSqlException(e);
            }
        }
    }

    private DDLTable toDdlTable(Table table, EntityNameProcessor entityNameProcessor)
    {
        final DDLTable ddlTable = new DDLTable();
        ddlTable.setName(entityNameProcessor.tableName(table.getName()));

        final List<DDLField> fields = newArrayList();
        for (Column column : table.getColumns())
        {
            fields.add(toDdlField(column, entityNameProcessor));
        }
        ddlTable.setFields(fields.toArray(new DDLField[fields.size()]));
        return ddlTable;
    }

    private DDLField toDdlField(Column column, EntityNameProcessor entityNameProcessor)
    {
        final DDLField ddlField = new DDLField();
        ddlField.setName(entityNameProcessor.columnName(column.getName()));
        ddlField.setType(TypeManager.getInstance().getType(getSqlType(column)));
        final Boolean pk = column.isPrimaryKey();
        if (pk != null)
        {
            ddlField.setPrimaryKey(pk);
        }
        final Boolean autoIncrement = column.isAutoIncrement();
        if (autoIncrement != null)
        {
            ddlField.setAutoIncrement(autoIncrement);
        }
        final Integer p = column.getPrecision();
        if (p != null)
        {
            ddlField.setPrecision(p);
        }

        final Integer s = column.getScale();
        if (s != null)
        {
            ddlField.setScale(s);
        }
        return ddlField;
    }

    private int getSqlType(Column column)
    {
        if (column.getSqlType() == Types.NUMERIC && column.getScale() > 0)
        {
            return Types.DOUBLE; // Oracle uses numeric for both floating point numbers and fixed numbers
        }
        return column.getSqlType();
    }
}
