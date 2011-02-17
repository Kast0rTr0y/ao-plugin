package com.atlassian.activeobjects.ao;

import net.java.ao.schema.CamelCaseFieldNameConverter;
import net.java.ao.schema.FieldNameConverter;

import java.lang.reflect.Method;

public final class ActiveObjectsFieldNameConverter implements FieldNameConverter
{
    private FieldNameConverter fieldNameConverter;

    public ActiveObjectsFieldNameConverter()
    {
        fieldNameConverter = new UpperCaseFieldNameConverter(new CamelCaseFieldNameConverter());
    }

    @Override
    public String getName(Method method)
    {
        return fieldNameConverter.getName(method);
    }

    @Override
    public String getPolyTypeName(Method method)
    {
        return fieldNameConverter.getPolyTypeName(method);
    }
}
