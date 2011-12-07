package com.atlassian.activeobjects;

import com.atlassian.plugin.PluginException;
import com.google.common.base.Predicate;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Common;
import net.java.ao.Polymorphic;
import net.java.ao.RawEntity;
import net.java.ao.db.OracleDatabaseProvider;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.TableNameConverter;

import java.lang.reflect.Method;
import java.util.Set;

import static com.google.common.collect.Iterables.any;

public final class NamesLengthAndOracleReservedWordsEntitiesValidator implements EntitiesValidator
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
        final String tableName = tableNameConverter.getName(entityClass);// will throw an exception if the entity name is too long
        if (isReservedWord(tableName))
        {
            throw new ActiveObjectsException("Entity class' '" + entityClass.getName() + "' table name is " + tableName + " which is a reserved word!");
        }
    }

    void checkColumnName(Method method, FieldNameConverter fieldNameConverter)
    {
        if (Common.isAccessor(method) || Common.isMutator(method))
        {
            final String columnName = fieldNameConverter.getName(method);
            if (isReservedWord(columnName))
            {
                throw new ActiveObjectsException("Method '" + method + "' column name is " + columnName + " which is a reserved word!");
            }
        }
    }

    private boolean isReservedWord(final String name)
    {
        return any(OracleDatabaseProvider.RESERVED_WORDS, new Predicate<String>()
        {
            @Override
            public boolean apply(String reservedWord)
            {
                return reservedWord.equalsIgnoreCase(name);
            }
        });
    }

    void checkPolymorphicColumnName(Method method, FieldNameConverter fieldNameConverter)
    {
        final Class<?> attributeTypeFromMethod = Common.getAttributeTypeFromMethod(method);
        if (attributeTypeFromMethod != null && attributeTypeFromMethod.isAnnotationPresent(Polymorphic.class))
        {
            final String polyTypeName = fieldNameConverter.getPolyTypeName(method);
            if (isReservedWord(polyTypeName))
            {
                throw new ActiveObjectsException("Method '" + method + "' polymorphic column name is " + polyTypeName + " which is a reserved word!");
            }
        }
    }
}