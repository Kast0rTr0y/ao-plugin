package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.exporter.TableReader;
import com.atlassian.dbexporter.jdbc.ImportExportSqlException;
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
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

final class ActiveObjectsTableReader implements TableReader
{
    private final DatabaseProvider provider;
    private final SchemaConfiguration schemaConfiguration;

    public ActiveObjectsTableReader(DatabaseProvider provider, SchemaConfiguration schemaConfiguration)
    {
        this.provider = checkNotNull(provider);
        this.schemaConfiguration = checkNotNull(schemaConfiguration);
    }

    public Iterable<Table> read(EntityNameProcessor entityNameProcessor)
    {
        final List<Table> tables = newArrayList();
        final DDLTable[] ddlTables;
        try
        {
            ddlTables = SchemaReader.readSchema(getConnection(), provider, schemaConfiguration, true);
        }
        catch (SQLException e)
        {
            throw new ImportExportSqlException("An error occurred reading schema information from database", e);
        }

        for (DDLTable ddlTable : ddlTables)
        {
            tables.add(readTable(ddlTable, entityNameProcessor));
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
            throw new ImportExportSqlException("Could not get connection from provider", e);
        }
    }

    private Table readTable(DDLTable ddlTable, EntityNameProcessor processor)
    {
        final String name = processor.tableName(ddlTable.getName());
        return new Table(name, readColumns(ddlTable.getFields(), processor), readForeignKeys(ddlTable.getForeignKeys()));
    }

    private List<Column> readColumns(DDLField[] fields, final EntityNameProcessor processor)
    {
        return Lists.transform(newArrayList(fields), new Function<DDLField, Column>()
        {
            public Column apply(DDLField field)
            {
                return readColumn(field, processor);
            }
        });
    }

    private Column readColumn(DDLField field, EntityNameProcessor processor)
    {
        final String name = processor.columnName(field.getName());
        return new Column(name, field.getType().getType(), field.isPrimaryKey(), field.isAutoIncrement(), field.getPrecision(), field.getScale());
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
