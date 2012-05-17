package com.atlassian.activeobjects.internal.db;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.java.ao.DisposableDataSource;
import net.java.ao.db.SQLServerDatabaseProvider;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLIndex;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.schema.ddl.SQLAction;

import static com.google.common.collect.Lists.newArrayList;

public class SQLServerCSDatabaseProvider extends SQLServerDatabaseProvider
{
    public SQLServerCSDatabaseProvider(DisposableDataSource dataSource)
    {
        this(dataSource, "dbo");
    }

    public SQLServerCSDatabaseProvider(DisposableDataSource dataSource, String schema)
    {
        super(dataSource, schema);
    }

    @Override
    protected Iterable<SQLAction> renderAlterTableChangeColumn(NameConverters nameConverters, DDLTable table, DDLField oldField, DDLField field)
    {
        final ImmutableList.Builder<SQLAction> sql = ImmutableList.builder();

        // Removing index before applying changes to columns, SQL Server doesn't like to touch columns with indexes!
        final Iterable<DDLIndex> indexes = findIndexesForField(table, field);
        for (DDLIndex index : indexes)
        {
            SQLAction sqlAction = renderDropIndex(nameConverters.getIndexNameConverter(), index);
            if (sqlAction != null)
            {
                sql.add(sqlAction);
            }
        }

        if (field.isPrimaryKey())
        {
            sql.add(SQLAction.of(new StringBuilder().append("ALTER TABLE ").append(withSchema(table.getName()))
                                         .append(" DROP CONSTRAINT ").append(primaryKeyName(table.getName(), field.getName()))));
        }

        sql.addAll(super.renderAlterTableChangeColumn(nameConverters, table, oldField, field));

        if (field.isPrimaryKey())
        {
            sql.add(SQLAction.of(new StringBuilder().append("ALTER TABLE ").append(withSchema(table.getName()))
                                         .append(" ADD CONSTRAINT ").append(primaryKeyName(table.getName(), field.getName()))
                                         .append(" PRIMARY KEY (").append(field.getName()).append(")")));
        }

        if ((field.getDefaultValue() != null && !field.getDefaultValue().equals(oldField.getDefaultValue())) || (field.getDefaultValue() == null && oldField.getDefaultValue() != null))
        {
            sql.add(SQLAction.of(new StringBuilder()
                                         .append("IF EXISTS (SELECT 1 FROM sys.objects WHERE NAME = ").append(renderValue(defaultConstraintName(table, field))).append(") ")
                                         .append("ALTER TABLE ").append(withSchema(table.getName()))
                                         .append(" DROP CONSTRAINT ").append(defaultConstraintName(table, field))));

            if (field.getDefaultValue() != null)
            {
                sql.add(SQLAction.of(new StringBuilder()
                                             .append("ALTER TABLE ").append(withSchema(table.getName()))
                                             .append(" ADD CONSTRAINT ").append(defaultConstraintName(table, field))
                                             .append(" DEFAULT ").append(renderValue(field.getDefaultValue()))
                                             .append(" FOR ").append(processID(field.getName()))));
            }
        }

        if (!oldField.isUnique() && field.isUnique())
        {
            sql.add(SQLAction.of(new StringBuilder()
                                         .append("ALTER TABLE ").append(withSchema(table.getName()))
                                         .append(" ADD CONSTRAINT ").append(nameConverters.getUniqueNameConverter().getName(table.getName(), field.getName()))
                                         .append(" UNIQUE(").append(processID(field.getName())).append(")")));
        }

        if (oldField.isUnique() && !field.isUnique())
        {
            sql.add(SQLAction.of(new StringBuilder()
                                         .append("ALTER TABLE ").append(withSchema(table.getName()))
                                         .append(" DROP CONSTRAINT ").append(nameConverters.getUniqueNameConverter().getName(table.getName(), oldField.getName()))));
        }

        // re-adding indexes!
        for (DDLIndex index : indexes)
        {
            sql.add(renderCreateIndex(nameConverters.getIndexNameConverter(), index));
        }

        return sql.build();
    }

    private String primaryKeyName(String tableName, String pkFieldName)
    {
        return "pk_" + tableName + "_" + pkFieldName;
    }

    private Iterable<DDLIndex> findIndexesForField(DDLTable table, final DDLField field)
    {
        return Iterables.filter(newArrayList(table.getIndexes()), new Predicate<DDLIndex>()
        {
            @Override
            public boolean apply(DDLIndex index)
            {
                return index.getField().equals(field.getName());
            }
        });
    }

    private String defaultConstraintName(DDLTable table, DDLField field)
    {
        return "df_" + table.getName() + '_' + field.getName();
    }
}
