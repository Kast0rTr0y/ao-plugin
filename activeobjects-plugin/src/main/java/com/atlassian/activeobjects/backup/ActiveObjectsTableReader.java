package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.exporter.TableReader;
import com.atlassian.dbexporter.jdbc.SqlRuntimeException;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import net.java.ao.DatabaseProvider;
import net.java.ao.SchemaConfiguration;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.schema.ddl.SchemaReader;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

final class ActiveObjectsTableReader implements TableReader
{
    private final DatabaseProvider provider;
    private final SchemaConfiguration schemaConfiguration;

    public ActiveObjectsTableReader(DatabaseProvider provider, SchemaConfiguration schemaConfiguration)
    {
        this.provider = checkNotNull(provider);
        this.schemaConfiguration = checkNotNull(schemaConfiguration);
    }

    public Iterable<Table> read(Context context)
    {
        final List<Table> tables = newArrayList();
        final DDLTable[] ddlTables;
        try
        {
            ddlTables = SchemaReader.readSchema(provider.getConnection(), provider, schemaConfiguration, true);
        }
        catch (SQLException e)
        {
            throw new SqlRuntimeException(e);
        }

        for (DDLTable ddlTable : ddlTables)
        {
            tables.add(readTable(ddlTable));
        }
        return tables;
    }

    private Table readTable(DDLTable ddlTable)
    {
        return new Table(ddlTable.getName(), readColumns(ddlTable.getFields()), readForeignKeys(ddlTable.getForeignKeys()));
    }

    private List<Column> readColumns(DDLField[] fields)
    {
        return Lists.transform(newArrayList(fields), new Function<DDLField, Column>()
        {
            public Column apply(DDLField field)
            {
                return readColumn(field);
            }
        });
    }

    private Column readColumn(DDLField field)
    {
        return new Column(field.getName(), field.getType().getType(), field.isPrimaryKey(), field.isAutoIncrement(), field.getPrecision());
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