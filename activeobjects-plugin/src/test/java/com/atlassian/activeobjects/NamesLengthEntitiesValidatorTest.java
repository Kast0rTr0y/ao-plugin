package com.atlassian.activeobjects;

import net.java.ao.ActiveObjectsException;
import net.java.ao.Polymorphic;
import net.java.ao.RawEntity;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.TableNameConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public final class NamesLengthEntitiesValidatorTest
{
    private static final Method GET_FIELD_METHOD = method(TestEntity.class, "getField");
    private static final Method RANDOM_METHOD = method(TestEntity.class, "randomMethod");
    private static final Method GET_ENTITY_METHOD = method(TestEntity.class, "getEntity");

    private NamesLengthEntitiesValidator validator;

    @Mock
    private TableNameConverter tableNameConverter;

    @Mock
    private FieldNameConverter fieldNameConverter;

    @Before
    public final void setUp()
    {
        validator = new NamesLengthEntitiesValidator();
    }

    @Test
    public void testCheckTableNameWithNoIssue()
    {
        final Class<TestEntity> entityClass = TestEntity.class;
        validator.checkTableName(entityClass, tableNameConverter);
        verify(tableNameConverter).getName(entityClass);
    }

    @Test(expected = ActiveObjectsException.class)
    public void testCheckTableNameWithException()
    {
        when(tableNameConverter.getName(TestEntity.class)).thenThrow(new ActiveObjectsException());
        validator.checkTableName(TestEntity.class, tableNameConverter);
    }

    @Test
    public void testCheckColumnNameWithNoIssue()
    {
        validator.checkColumnName(GET_FIELD_METHOD, fieldNameConverter);
        verify(fieldNameConverter).getName(GET_FIELD_METHOD);
    }

    @Test
    public void testCheckColumnNameWithRandomMethod()
    {
        validator.checkColumnName(RANDOM_METHOD, fieldNameConverter);
        verifyZeroInteractions(fieldNameConverter);
    }

    @Test(expected = ActiveObjectsException.class)
    public void testCheckColumnNameWithException()
    {
        when(fieldNameConverter.getName(GET_FIELD_METHOD)).thenThrow(new ActiveObjectsException());
        validator.checkColumnName(GET_FIELD_METHOD, fieldNameConverter);
    }

    @Test
    public void testCheckPolymorphicColumnNameNoIssue()
    {
        validator.checkPolymorphicColumnName(GET_ENTITY_METHOD, fieldNameConverter);
        verify(fieldNameConverter).getPolyTypeName(GET_ENTITY_METHOD);
    }

    @Test(expected = ActiveObjectsException.class)
    public void testCheckPolymorphicColumnNameWithException()
    {
        when(fieldNameConverter.getPolyTypeName(GET_ENTITY_METHOD)).thenThrow(new ActiveObjectsException());
        validator.checkPolymorphicColumnName(GET_ENTITY_METHOD, fieldNameConverter);
    }

    @Test
    public void testCheckPolymorphicColumnNameNonPolymorphic()
    {
        validator.checkPolymorphicColumnName(GET_FIELD_METHOD, fieldNameConverter);
        verifyZeroInteractions(fieldNameConverter);
    }

    private static Method method(Class<?> type, String name)
    {
        try
        {
            return type.getMethod(name);
        }
        catch (NoSuchMethodException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static interface TestEntity extends RawEntity<Object>
    {
        int getField();

        void randomMethod();

        PolymorphicEntity getEntity();
    }

    @Polymorphic
    private static interface PolymorphicEntity extends RawEntity<Object>
    {
    }
}
