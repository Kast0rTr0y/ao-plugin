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

import static com.atlassian.activeobjects.ao.ConverterUtils.*;
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
        final String name = fieldNameConverter.getName(method);
        checkLength(name,
                "Invalid entity, generated field name (" + name + ") for method '" +
                        method.getDeclaringClass().getClass() + "#"+ method.getName() + "' is too long! " +
                        "It should be no longer than " + MAX_LENGTH + " chars.");
        return name;
    }

    @Override
    public String getPolyTypeName(Method method)
    {
        final String name = fieldNameConverter.getPolyTypeName(method);
        checkLength(name,
                "Invalid entity, generated field polymorphic type name (" + name + ") for method '" +
                        method.getDeclaringClass().getClass() + "#"+ method.getName() + "' is too long! " +
                        "It should be no longer than " + MAX_LENGTH + " chars.");
        return name;
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
