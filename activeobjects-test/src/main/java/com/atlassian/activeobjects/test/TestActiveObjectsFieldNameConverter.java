package com.atlassian.activeobjects.test;

import com.atlassian.activeobjects.ao.ActiveObjectsFieldNameConverter;
import net.java.ao.schema.FieldNameConverter;

import java.lang.reflect.Method;

public final class TestActiveObjectsFieldNameConverter implements FieldNameConverter
{
    private final FieldNameConverter fnc = new ActiveObjectsFieldNameConverter();

    @Override
    public String getName(Method method)
    {
        return fnc.getName(method);
    }

    @Override
    public String getPolyTypeName(Method method)
    {
        return fnc.getPolyTypeName(method);
    }
}
