package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.DatabaseInformations;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.importer.TableCreator;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import net.java.ao.DatabaseProvider;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.types.TypeInfo;
import net.java.ao.types.TypeManager;
import net.java.ao.types.TypeQualifiers;
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
    private final Logger logger = LoggerFactory.getLogger("net.java.ao.sql");

    private final ImportExportErrorService errorService;
    private final DatabaseProvider provider;
    private final NameConverters converters;

    public ActiveObjectsTableCreator(ImportExportErrorService errorService, DatabaseProvider provider, NameConverters converters)
    {
        this.errorService = checkNotNull(errorService);
        this.provider = checkNotNull(provider);
        this.converters = checkNotNull(converters);
    }

    public void create(DatabaseInformation databaseInformation, Iterable<Table> tables, EntityNameProcessor entityNameProcessor, ProgressMonitor monitor)
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
                create(DatabaseInformations.database(databaseInformation), stmt, table, entityNameProcessor);
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

    private void create(DatabaseInformations.Database db, Statement stmt, Table table, EntityNameProcessor entityNameProcessor)
    {
        final DDLAction a = new DDLAction(DDLActionType.CREATE);
        a.setTable(toDdlTable(exportTypeManager(db), entityNameProcessor, table));
        final String[] sqlStatements = provider.renderAction(converters, a);
        for (String sql : sqlStatements)
        {
            try
            {

                logger.debug(sql);
                stmt.executeUpdate(sql);
            }
            catch (SQLException e)
            {
                throw errorService.newImportExportSqlException(table.getName(), "The following sql caused an error:\n" + sql + "\n---\n", e);
            }
        }
    }

    private DDLTable toDdlTable(TypeManager exportTypeManager, EntityNameProcessor entityNameProcessor, Table table)
    {
        final DDLTable ddlTable = new DDLTable();
        ddlTable.setName(entityNameProcessor.tableName(table.getName()));

        final List<DDLField> fields = newArrayList();
        for (Column column : table.getColumns())
        {
            fields.add(toDdlField(exportTypeManager, entityNameProcessor, column));
        }
        ddlTable.setFields(fields.toArray(new DDLField[fields.size()]));
        return ddlTable;
    }

    private DDLField toDdlField(TypeManager exportTypeManager, EntityNameProcessor entityNameProcessor, Column column)
    {
        final DDLField ddlField = new DDLField();
        ddlField.setName(entityNameProcessor.columnName(column.getName()));

        TypeInfo<?> typeFromSchema = getTypeInfo(exportTypeManager, column);
        ddlField.setType(typeFromSchema);
        ddlField.setJdbcType(typeFromSchema.getJdbcWriteType());

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
        return ddlField;
    }

    private TypeInfo<?> getTypeInfo(TypeManager exportTypeManager, Column column)
    {
        final TypeQualifiers qualifiers = getQualifiers(column);
        final TypeInfo<?> exportedType = exportTypeManager.getTypeFromSchema(column.getSqlType(), qualifiers);

        final Class<?> javaType = exportedType.getLogicalType().getTypes().iterator().next();

        TypeInfo<?> type = provider.getTypeManager().getType(javaType, exportedType.getQualifiers());
        return type;
    }

    private TypeQualifiers getQualifiers(Column column)
    {
        TypeQualifiers qualifiers = TypeQualifiers.qualifiers();
        if (column.getSqlType() == Types.NUMERIC && column.getPrecision() != null && column.getPrecision() > 0)
        {
            qualifiers = qualifiers.precision(column.getPrecision());
        }
        else if (isString(column))
        {
            qualifiers = qualifiers.stringLength(column.getPrecision());
        }
        if (column.getSqlType() == Types.NUMERIC && column.getScale() != null && column.getScale() > 0)
        {
            qualifiers = qualifiers.scale(column.getScale());
        }

        return qualifiers;
    }

    private boolean isString(Column column)
    {
        final int sqlType = column.getSqlType();
        return
                sqlType == Types.CHAR
                        || sqlType == Types.LONGNVARCHAR
                        || sqlType == Types.NCHAR
                        || sqlType == Types.VARCHAR
                        || sqlType == Types.CLOB
                        || sqlType == Types.NCLOB;
    }

    /**
     * Retrives the type managers of the export from the read database info.
     */
    private TypeManager exportTypeManager(DatabaseInformations.Database db)
    {
        switch (db.getType())
        {
            case HSQL:
                return TypeManager.hsql();
            case MYSQL:
                return TypeManager.mysql();
            case POSTGRES:
                return TypeManager.postgres();
            case MSSQL:
                return TypeManager.sqlServer();
            case ORACLE:
                return TypeManager.oracle();
            case UNKNOWN:
            default:
                throw errorService.newImportExportException(null, "Could not determine the source database");
        }
    }
}
