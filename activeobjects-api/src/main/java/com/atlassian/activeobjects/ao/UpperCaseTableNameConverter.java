package com.atlassian.activeobjects.ao;

import net.java.ao.RawEntity;
import net.java.ao.schema.TableNameConverter;

import static com.atlassian.activeobjects.ao.ConverterUtils.*;
import static com.google.common.base.Preconditions.*;

final class UpperCaseTableNameConverter implements TableNameConverter
{
    private final TableNameConverter delegate;

    public UpperCaseTableNameConverter(TableNameConverter delegate)
    {
        this.delegate = checkNotNull(delegate);
    }

    public String getName(Class<? extends RawEntity<?>> clazz)
    {
        return toUpperCase(delegate.getName(clazz));
    }
}
