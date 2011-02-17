package com.atlassian.activeobjects.ao;

import net.java.ao.RawEntity;
import net.java.ao.schema.AbstractTableNameConverter;

import static com.google.common.base.Preconditions.*;

final class ClassNameTableNameConverter extends AbstractTableNameConverter
{
    @Override
    protected String convertName(Class<? extends RawEntity<?>> entity)
    {
        return checkNotNull(entity.getSimpleName());
    }
}
