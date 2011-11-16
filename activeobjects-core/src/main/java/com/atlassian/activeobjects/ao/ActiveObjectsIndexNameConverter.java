package com.atlassian.activeobjects.ao;

import net.java.ao.schema.DefaultIndexNameConverter;
import net.java.ao.schema.IndexNameConverter;

import static com.google.common.base.Preconditions.*;
import static net.java.ao.Common.*;

public final class ActiveObjectsIndexNameConverter implements IndexNameConverter
{
    private final IndexNameConverter delegate;

    public ActiveObjectsIndexNameConverter()
    {
        this(new DefaultIndexNameConverter());
    }

    public ActiveObjectsIndexNameConverter(IndexNameConverter delegate)
    {
        this.delegate = checkNotNull(delegate);
    }

    @Override
    public String getName(String tableName, String columnName)
    {
        return shorten(delegate.getName(tableName, columnName), ConverterUtils.MAX_LENGTH);
    }
}
