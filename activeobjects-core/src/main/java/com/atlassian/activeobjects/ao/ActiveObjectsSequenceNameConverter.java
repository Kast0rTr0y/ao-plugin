package com.atlassian.activeobjects.ao;

import net.java.ao.schema.DefaultSequenceNameConverter;
import net.java.ao.schema.SequenceNameConverter;

import static com.google.common.base.Preconditions.*;
import static net.java.ao.Common.*;

public final class ActiveObjectsSequenceNameConverter implements SequenceNameConverter
{
    private final SequenceNameConverter delegate;

    public ActiveObjectsSequenceNameConverter()
    {
        this(new DefaultSequenceNameConverter());
    }

    public ActiveObjectsSequenceNameConverter(SequenceNameConverter delegate)
    {
        this.delegate = checkNotNull(delegate);
    }

    @Override
    public String getName(String tableName, String columnName)
    {
        return shorten(delegate.getName(tableName, columnName), ConverterUtils.MAX_LENGTH);
    }
}
