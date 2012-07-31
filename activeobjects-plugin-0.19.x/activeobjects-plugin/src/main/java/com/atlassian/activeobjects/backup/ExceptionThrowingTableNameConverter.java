package com.atlassian.activeobjects.backup;

import net.java.ao.RawEntity;
import net.java.ao.schema.TableNameConverter;

public final class ExceptionThrowingTableNameConverter implements TableNameConverter
{
    @Override
    public String getName(Class<? extends RawEntity<?>> aClass)
    {
        throw new IllegalStateException();
    }
}
