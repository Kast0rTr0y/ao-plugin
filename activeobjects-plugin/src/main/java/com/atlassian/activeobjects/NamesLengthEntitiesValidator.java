package com.atlassian.activeobjects;

import com.atlassian.plugin.PluginException;
import net.java.ao.Common;
import net.java.ao.Polymorphic;
import net.java.ao.RawEntity;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.TableNameConverter;

import java.lang.reflect.Method;
import java.util.Set;

public final class NamesLengthEntitiesValidator implements EntitiesValidator
{
    private static final int MAX_NUMBER_OF_ENTITIES = 50;

    @Override
    public Set<Class<? extends RawEntity<?>>> check(Set<Class<? extends RawEntity<?>>> entityClasses, NameConverters nameConverters)
    {
        if (entityClasses.size() > MAX_NUMBER_OF_ENTITIES)
        {
            throw new PluginException("Plugins are allowed no more than " + MAX_NUMBER_OF_ENTITIES + " entities!");
        }
        for (Class<? extends RawEntity<?>> entityClass : entityClasses)
        {
            check(entityClass, nameConverters);
        }
        return entityClasses;
    }

    void check(Class<? extends RawEntity<?>> entityClass, NameConverters nameConverters)
    {
        checkTableName(entityClass, nameConverters.getTableNameConverter());

        final FieldNameConverter fieldNameConverter = nameConverters.getFieldNameConverter();
        for (Method method : entityClass.getMethods())
        {
            checkColumnName(method, fieldNameConverter);
            checkPolymorphicColumnName(method, fieldNameConverter);
        }
    }

    void checkTableName(Class<? extends RawEntity<?>> entityClass, TableNameConverter tableNameConverter)
    {
        tableNameConverter.getName(entityClass); // will throw an exception if the entity name is too long
    }

    void checkColumnName(Method method, FieldNameConverter fieldNameConverter)
    {
        if (Common.isAccessor(method) || Common.isMutator(method))
        {
            fieldNameConverter.getName(method);
        }
    }

    void checkPolymorphicColumnName(Method method, FieldNameConverter fieldNameConverter)
    {
        final Class<?> attributeTypeFromMethod = Common.getAttributeTypeFromMethod(method);
        if (attributeTypeFromMethod != null && attributeTypeFromMethod.isAnnotationPresent(Polymorphic.class))
        {
            fieldNameConverter.getPolyTypeName(method);
        }
    }
}
