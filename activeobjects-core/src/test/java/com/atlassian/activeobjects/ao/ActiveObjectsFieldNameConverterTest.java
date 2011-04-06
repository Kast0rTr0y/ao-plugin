package com.atlassian.activeobjects.ao;

import net.java.ao.Accessor;
import net.java.ao.ManyToMany;
import net.java.ao.Mutator;
import net.java.ao.OneToMany;
import net.java.ao.OneToOne;
import net.java.ao.RawEntity;
import net.java.ao.schema.FieldNameConverter;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public final class ActiveObjectsFieldNameConverterTest
{
    private static final String NAME_OF_ANNOTATED_METHOD = "nameOfAnnotatedMethod";
    private static final String CONVERTED_NAME_OF_ANNOTATED_METHOD = "NAME_OF_ANNOTATED_METHOD_ID";

    private FieldNameConverter converter;

    @Before
    public void setUp()
    {
        converter = new ActiveObjectsFieldNameConverter();
    }

    @Test
    public void getNameForMethodAnnotatedWithOneToOne() throws Exception
    {
        assertNull(converter.getName(method("oneToOne")));
    }

    @Test
    public void getNameForMethodAnnotatedWithOneToMany() throws Exception
    {
        assertNull(converter.getName(method("oneToMany")));
    }

    @Test
    public void getNameForMethodAnnotatedWithManyToMany() throws Exception
    {
        assertNull(converter.getName(method("manyToMany")));
    }

    @Test
    public void getNameForMethodAnnotatedWithMutator() throws Exception
    {
        assertEquals(CONVERTED_NAME_OF_ANNOTATED_METHOD, converter.getName(method("changeThis", SomeInterface.class)));
    }

    @Test
    public void getNameForMethodAnnotatedWithAccessor() throws Exception
    {
        assertEquals(CONVERTED_NAME_OF_ANNOTATED_METHOD, converter.getName(method("accessThis")));
    }

    @Test
    public void getNameForSetterMethodWithEntity() throws Exception
    {
        assertEquals("THIS_ID", converter.getName(method("setThis", SomeInterface.class)));
    }

    @Test
    public void getNameForIsMethod() throws Exception
    {
        assertEquals("THIS_ID", converter.getName(method("isThis")));
    }

    @Test
    public void getNameForGetterMethod() throws Exception
    {
        assertEquals("THIS_ID", converter.getName(method("getThis")));
    }

        private static Method method(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException
    {
        return ClassWithMethods.class.getMethod(methodName, parameterTypes);
    }

    private static final class ClassWithMethods
    {
        @SuppressWarnings("unused")
        public SomeInterface getThis()
        {
            return null;
        }

        @SuppressWarnings("unused")
        public SomeInterface isThis()
        {
            return null;
        }

        @SuppressWarnings("unused")
        public void setThis(SomeInterface si)
        {
        }

        @Accessor(NAME_OF_ANNOTATED_METHOD)
        @SuppressWarnings("unused")
        public SomeInterface accessThis()
        {
            return null;
        }

        @Mutator(NAME_OF_ANNOTATED_METHOD)
        @SuppressWarnings("unused")
        public void changeThis(SomeInterface si)
        {
        }

        @OneToOne
        @SuppressWarnings("unused")
        public void oneToOne()
        {
        }

        @OneToMany
        @SuppressWarnings("unused")
        public void oneToMany()
        {
        }

        @ManyToMany(value = SomeInterface.class, where = "")
        @SuppressWarnings("unused")
        public void manyToMany()
        {
        }
    }

    private static interface SomeInterface extends RawEntity<Object>
    {
    }
}
