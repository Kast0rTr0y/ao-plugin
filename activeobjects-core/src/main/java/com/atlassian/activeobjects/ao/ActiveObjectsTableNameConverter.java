package com.atlassian.activeobjects.ao;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.RawEntity;
import net.java.ao.schema.TableNameConverter;

import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsTableNameConverter implements TableNameConverter
{
    private final TableNameConverter tableNameConverter;

    public ActiveObjectsTableNameConverter(Prefix prefix)
    {
        tableNameConverter = new PrefixedTableNameConverter(checkNotNull(prefix), new UpperCaseTableNameConverter(new ClassNameTableNameConverter()));
    }

    @Override
    public String getName(Class<? extends RawEntity<?>> clazz)
    {
        return tableNameConverter.getName(clazz);
    }
}
