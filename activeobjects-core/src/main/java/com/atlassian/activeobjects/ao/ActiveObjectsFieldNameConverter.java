package com.atlassian.activeobjects.ao;

import net.java.ao.schema.AccessorFieldNameResolver;
import net.java.ao.schema.Case;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.FieldNameResolver;
import net.java.ao.schema.GetterFieldNameResolver;
import net.java.ao.schema.IsAFieldNameResolver;
import net.java.ao.schema.MutatorFieldNameResolver;
import net.java.ao.schema.NullFieldNameResolver;
import net.java.ao.schema.PrimaryKeyFieldNameResolver;
import net.java.ao.schema.RelationalFieldNameResolver;
import net.java.ao.schema.SetterFieldNameResolver;
import net.java.ao.schema.UnderscoreFieldNameConverter;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;

public final class ActiveObjectsFieldNameConverter implements FieldNameConverter
{
    private FieldNameConverter fieldNameConverter;

    public ActiveObjectsFieldNameConverter()
    {
        fieldNameConverter = new UnderscoreFieldNameConverter(Case.UPPER, newArrayList(
                new RelationalFieldNameResolver(),
                new TransformingFieldNameResolver(new MutatorFieldNameResolver()),
                new TransformingFieldNameResolver(new AccessorFieldNameResolver()),
                new TransformingFieldNameResolver(new PrimaryKeyFieldNameResolver()),
                new GetterFieldNameResolver(),
                new SetterFieldNameResolver(),
                new IsAFieldNameResolver(),
                new NullFieldNameResolver()
        ));
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

    private static final class TransformingFieldNameResolver implements FieldNameResolver
    {
        private final FieldNameResolver delegate;

        public TransformingFieldNameResolver(FieldNameResolver delegate)
        {
            this.delegate = checkNotNull(delegate);
        }

        @Override
        public boolean accept(Method method)
        {
            return delegate.accept(method);
        }

        @Override
        public String resolve(Method method)
        {
            return delegate.resolve(method);
        }

        @Override
        public boolean transform()
        {
            return true;
        }
    }
}
