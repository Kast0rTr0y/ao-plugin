package com.atlassian.activeobjects;

import com.atlassian.activeobjects.external.IgnoreReservedKeyword;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Polymorphic;
import net.java.ao.RawEntity;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.Ignore;
import net.java.ao.schema.TableNameConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;

import static com.atlassian.activeobjects.NamesLengthAndOracleReservedWordsEntitiesValidator.RESERVED_WORDS;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public final class NamesLengthAndOracleReservedWordsEntitiesValidatorTest
{
    private static final Method GET_FIELD_METHOD = method(TestEntity.class, "getField");
    private static final Method IGNORE_METHOD = method(TestEntity.class, "getIgnoreMethod");
    private static final Method IGNORE_RESERVED_KEYWORD_METHOD = method(TestEntity.class, "getIgnoreReservedKeywordMethod");
    private static final Method RANDOM_METHOD = method(TestEntity.class, "randomMethod");
    private static final Method GET_ENTITY_METHOD = method(TestEntity.class, "getEntity");

    private NamesLengthAndOracleReservedWordsEntitiesValidator validator;

    @Mock
    private TableNameConverter tableNameConverter;

    @Mock
    private FieldNameConverter fieldNameConverter;

    @Before
    public final void setUp()
    {
        validator = new NamesLengthAndOracleReservedWordsEntitiesValidator();
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

    @Test
    public void testCheckTableNameIsOracleKeyword()
    {
        for (String oracleReservedWord : RESERVED_WORDS)
        {
            when(tableNameConverter.getName(TestEntity.class)).thenReturn(oracleReservedWord);
            try
            {
                validator.checkTableName(TestEntity.class, tableNameConverter);
                fail("The validator should have thrown an exception for table named '" + oracleReservedWord + "' which is an Oracle key word.");
            }
            catch (ActiveObjectsException e)
            {
                // expected
            }
        }
    }

    @Test
    public void testCheckFieldNameIsOracleKeyword()
    {
        for (String oracleReservedWord : RESERVED_WORDS)
        {
            when(fieldNameConverter.getName(GET_FIELD_METHOD)).thenReturn(oracleReservedWord);
            try
            {
                validator.checkColumnName(GET_FIELD_METHOD, fieldNameConverter);
                fail("The validator should have thrown an exception for field/column named '" + oracleReservedWord + "' which is an Oracle key word.");
            }
            catch (ActiveObjectsException e)
            {
                // expected
            }
        }
    }

    @Test
    public void testCheckFieldNameIsOracleKeywordAndMethodIsAnnotatedIgnore()
    {
        for (String oracleReservedWord : RESERVED_WORDS)
        {
            when(fieldNameConverter.getName(IGNORE_METHOD)).thenReturn(oracleReservedWord);
            try
            {
                validator.checkColumnName(IGNORE_METHOD, fieldNameConverter);
            }
            catch (ActiveObjectsException e)
            {
                fail("The validator should NOT have thrown an exception for field/column named '" + oracleReservedWord + "' which is an Oracle key word.");
            }
        }
    }

    @Test
    public void testCheckFieldNameIsOracleKeywordAndMethodIsAnnotatedIgnoreReservedKeyword()
    {
        for (String oracleReservedWord : RESERVED_WORDS)
        {
            when(fieldNameConverter.getName(IGNORE_RESERVED_KEYWORD_METHOD)).thenReturn(oracleReservedWord);
            try
            {
                validator.checkColumnName(IGNORE_RESERVED_KEYWORD_METHOD, fieldNameConverter);
            }
            catch (ActiveObjectsException e)
            {
                fail("The validator should NOT have thrown an exception for field/column named '" + oracleReservedWord + "' which is an Oracle key word.");
            }
        }
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

        @Ignore
        String getIgnoreMethod();

        @IgnoreReservedKeyword
        String getIgnoreReservedKeywordMethod();
    }

    @Polymorphic
    private static interface PolymorphicEntity extends RawEntity<Object>
    {
    }
}
