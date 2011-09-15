package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.exporter.TableReader;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import net.java.ao.DatabaseProvider;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.schema.ddl.SchemaReader;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.List;

import static com.atlassian.dbexporter.DatabaseInformations.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

final class ActiveObjectsTableReader implements TableReader
{
    public static final int DEFAULT_SCALE = -1;
    public static final int DEFAULT_PRECISION = -1;
    public static final int DEFAULT_MYSQL_DOUBLE_PRECISION = 22;
    public static final int DEFAULT_MYSQL_DOUBLE_SCALE = 31;
    public static final int DEFAULT_POSTGRES_PRECISION = 17;
    public static final int DEFAULT_POSTGRES_SCALE = 17;
    public static final int POSTGRES_PRECISION_FOR_TEXT = 2147483647;

    private final ImportExportErrorService errorService;
    private final DatabaseProvider provider;
    private final SchemaConfiguration schemaConfiguration;

    public ActiveObjectsTableReader(ImportExportErrorService errorService, DatabaseProvider provider, SchemaConfiguration schemaConfiguration)
    {
        this.errorService = checkNotNull(errorService);
        this.provider = checkNotNull(provider);
        this.schemaConfiguration = checkNotNull(schemaConfiguration);
    }

    @Override
    public Iterable<Table> read(DatabaseInformation databaseInformation, EntityNameProcessor entityNameProcessor)
    {
        final List<Table> tables = newArrayList();
        final DDLTable[] ddlTables;
        try
        {
            ddlTables = SchemaReader.readSchema(getConnection(), provider, schemaConfiguration, true);
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "An error occurred reading schema information from database", e);
        }

        for (DDLTable ddlTable : ddlTables)
        {
            tables.add(readTable(databaseInformation, ddlTable, entityNameProcessor));
        }
        return tables;
    }

    private Connection getConnection()
    {
        try
        {
            return provider.getConnection();
        }
        catch (SQLException e)
        {
            throw errorService.newImportExportSqlException(null, "Could not get connection from provider", e);
        }
    }

    private Table readTable(DatabaseInformation databaseInformation, DDLTable ddlTable, EntityNameProcessor processor)
    {
        final String name = processor.tableName(ddlTable.getName());
        return new Table(name, readColumns(databaseInformation, ddlTable.getFields(), processor), readForeignKeys(ddlTable.getForeignKeys()));
    }

    private List<Column> readColumns(final DatabaseInformation info, DDLField[] fields, final EntityNameProcessor processor)
    {
        return Lists.transform(newArrayList(fields), new Function<DDLField, Column>()
        {
            @Override
            public Column apply(DDLField field)
            {
                return readColumn(info, field, processor);
            }
        });
    }

    private Column readColumn(DatabaseInformation info, DDLField field, EntityNameProcessor processor)
    {
        final String name = processor.columnName(field.getName());
        return
                new Column(name, getType(info, field), field.isPrimaryKey(), field.isAutoIncrement(),
                        getPrecision(info, field), getScale(info, field));
    }

    private int getType(DatabaseInformation info, DDLField field)
    {
        if (isPostgresVarcharForText(info, field))
        {
            return Types.CLOB;
        }

        return field.getType().getType();
    }

    private int getScale(DatabaseInformation info, DDLField field)
    {
        if (isHsqlDouble(info, field)
                || isDefaultPostgresPrecisionForDouble(info, field)
                || isDefaultMySqlPrecisionForDouble(info, field))
        {
            return DEFAULT_SCALE;
        }


        return field.getScale();
    }

    private int getPrecision(DatabaseInformation info, DDLField field)
    {
        if (isBigInt(field)
                || isLongVarchar(field)
                || isHsqlDouble(info, field)
                || isSqlServerClob(info, field)
                || isDefaultPostgresPrecisionForDouble(info, field)
                || isDefaultMySqlPrecisionForDouble(info, field)
                || isPostgresVarcharForText(info, field))
        {
            return DEFAULT_PRECISION;
        }
        return field.getPrecision();
    }

    private boolean isBigInt(DDLField field)
    {
        return field.getType().getType() == Types.BIGINT;
    }

    private boolean isHsqlDouble(DatabaseInformation info, DDLField field)
    {
        return isDatabase(info, Database.Type.HSQL)
                && isFieldOfType(field, Types.DOUBLE);
    }

    private boolean isSqlServerClob(DatabaseInformation info, DDLField field)
    {
        return isDatabase(info, Database.Type.MSSQL)
                && isFieldOfType(field, Types.CLOB);
    }

    private boolean isLongVarchar(DDLField field)
    {
        return isFieldOfType(field, Types.LONGVARCHAR);
    }

    private boolean isDefaultPostgresPrecisionForDouble(DatabaseInformation info, DDLField field)
    {
        return isPostgresDouble(info, field)
                && field.getPrecision() == DEFAULT_POSTGRES_PRECISION
                && field.getScale() == DEFAULT_POSTGRES_SCALE;
    }

    private boolean isPostgresDouble(DatabaseInformation info, DDLField field)
    {
        return isDatabase(info, Database.Type.POSTGRES)
                && isFieldOfType(field, Types.DOUBLE);
    }

    private boolean isDefaultMySqlPrecisionForDouble(DatabaseInformation info, DDLField field)
    {
        return isDatabase(info, Database.Type.MYSQL)
                && isFieldOfType(field, Types.DOUBLE)
                && field.getPrecision() == DEFAULT_MYSQL_DOUBLE_PRECISION
                && field.getScale() == DEFAULT_MYSQL_DOUBLE_SCALE;
    }

    private boolean isPostgresVarcharForText(DatabaseInformation info, DDLField field)
    {
        return isDatabase(info, Database.Type.POSTGRES)
                && isFieldOfType(field, Types.VARCHAR)
                && field.getPrecision() == POSTGRES_PRECISION_FOR_TEXT;
    }

    private boolean isFieldOfType(DDLField field, int aDouble)
    {
        return field.getType().getType() == aDouble;
    }

    private boolean isDatabase(DatabaseInformation information, Database.Type dbType)
    {
        return database(information).getType().equals(dbType);
    }

    private Collection<ForeignKey> readForeignKeys(DDLForeignKey[] foreignKeys)
    {
        return Collections2.transform(newArrayList(foreignKeys), new Function<DDLForeignKey, ForeignKey>()
        {
            public ForeignKey apply(DDLForeignKey fk)
            {
                return readForeignKey(fk);
            }
        });
    }

    private ForeignKey readForeignKey(DDLForeignKey fk)
    {
        return new ForeignKey(fk.getFKName(), fk.getDomesticTable(), fk.getField(), fk.getTable(), fk.getForeignField());
    }
}
