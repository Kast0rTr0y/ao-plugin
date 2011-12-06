package com.atlassian.activeobjects.ao;

import net.java.ao.schema.DefaultUniqueNameConverter;
import net.java.ao.schema.UniqueNameConverter;

import static com.google.common.base.Preconditions.*;
import static net.java.ao.Common.*;

public final class ActiveObjectsUniqueNameConverter implements UniqueNameConverter
{
    private final UniqueNameConverter delegate;

    public ActiveObjectsUniqueNameConverter()
    {
        this(new DefaultUniqueNameConverter());
    }

    public ActiveObjectsUniqueNameConverter(UniqueNameConverter delegate)
    {
        this.delegate = checkNotNull(delegate);
    }

    @Override
    public String getName(String tableName, String columnName)
    {
        return shorten(delegate.getName(tableName, columnName), ConverterUtils.MAX_LENGTH);
    }
}
