package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.importer.TableCreator;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import net.java.ao.DatabaseProvider;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.types.TypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final int DEFAULT_PRECISION = -1;
    private static final int MAX_MYSQL_SCALE = 30;
    private static final int ORACLE_NUMERIC_BIGINT_PRECISION = 20;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ImportExportErrorService errorService;
    private final DatabaseProvider provider;

    public ActiveObjectsTableCreator(ImportExportErrorService errorService, DatabaseProvider provider)
    {
        this.errorService = checkNotNull(errorService);
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
            throw errorService.newImportExportSqlException(null, "", e);
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
                throw errorService.newImportExportSqlException(table.getName(), "The following sql caused an error:\n" + sql + "\n---\n", e);
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
        final Integer p = getPrecision(column);
        if (p != null)
        {
            ddlField.setPrecision(p);
        }

        final Integer s = getScale(column);
        if (s != null)
        {
            ddlField.setScale(s);
        }
        return ddlField;
    }

    private int getSqlType(Column column)
    {
        if (isOracleNumericForDouble(column))
        {
            return Types.DOUBLE;
        }

        if (isOracleNumericForBigInt(column))
        {
            return Types.BIGINT;
        }

        if (isMySqlBoolean(column))
        {
            return Types.BOOLEAN;
        }
        return column.getSqlType();
    }

    private Integer getPrecision(Column column)
    {
        if (isOracleNumericForBigInt(column))
        {
            return DEFAULT_PRECISION;
        }
        return column.getPrecision();
    }

    private Integer getScale(Column column)
    {
        Integer scale = column.getScale();
        if (scale == null)
        {
            return null;
        }

        final Integer precision = column.getPrecision();
        if (precision != null && scale > precision)
        {
            logger.warn("Scale is greater than precision (" + scale + " > " + precision + "), which is not allowed in most databases, setting scale with same value as precision");
            scale = precision;
        }

        if (scale > MAX_MYSQL_SCALE)
        {
            logger.warn("Scale is set to a value greater than 30 (" + scale + "), which is not compatible with MySQL 5, setting actual value to 30.");
            return MAX_MYSQL_SCALE;
        }
        return scale;
    }

    private static boolean isOracleNumericForBigInt(Column column)
    {
        return column.getSqlType() == Types.NUMERIC && column.getPrecision() == ORACLE_NUMERIC_BIGINT_PRECISION;
    }

    private static boolean isOracleNumericForDouble(Column column)
    {
        return column.getSqlType() == Types.NUMERIC && column.getScale() != null && column.getScale() > 0;
    }

    private static boolean isMySqlBoolean(Column column)
    {
        return Types.BIT == column.getSqlType() && column.getPrecision() != null && column.getPrecision() == 1;
    }
}
