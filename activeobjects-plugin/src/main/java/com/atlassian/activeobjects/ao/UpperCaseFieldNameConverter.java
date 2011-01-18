package com.atlassian.activeobjects.ao;

import net.java.ao.schema.FieldNameConverter;

import java.lang.reflect.Method;

import static com.atlassian.activeobjects.ao.ConverterUtils.*;
import static com.google.common.base.Preconditions.*;

public final class UpperCaseFieldNameConverter implements FieldNameConverter
{
    private final FieldNameConverter delegate;

    public UpperCaseFieldNameConverter(FieldNameConverter delegate)
    {
        this.delegate = checkNotNull(delegate);
    }

    public String getName(Method method)
    {
        return toUpperCase(delegate.getName(method));
    }

    public String getPolyTypeName(Method method)
    {
        return toUpperCase(delegate.getPolyTypeName(method));
    }
}
