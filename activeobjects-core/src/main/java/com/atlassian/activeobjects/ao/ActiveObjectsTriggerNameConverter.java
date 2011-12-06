package com.atlassian.activeobjects.ao;

import net.java.ao.schema.DefaultTriggerNameConverter;
import net.java.ao.schema.TriggerNameConverter;

import static com.google.common.base.Preconditions.*;
import static net.java.ao.Common.*;

public final class ActiveObjectsTriggerNameConverter implements TriggerNameConverter
{
    private final TriggerNameConverter delegate;

    public ActiveObjectsTriggerNameConverter()
    {
        this(new DefaultTriggerNameConverter());
    }

    public ActiveObjectsTriggerNameConverter(TriggerNameConverter delegate)
    {
        this.delegate = checkNotNull(delegate);
    }

    @Override
    public String autoIncrementName(String tableName, String columnName)
    {
        return shorten(delegate.autoIncrementName(tableName, columnName), ConverterUtils.MAX_LENGTH);
    }
}
