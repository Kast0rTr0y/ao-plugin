package com.atlassian.activeobjects.ao;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.RawEntity;
import net.java.ao.schema.Table;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ActiveObjectsTableNameConverterTest
{
    private static final String PREFIX = "PFX";

    private ActiveObjectsTableNameConverter converter;

    @Before
    public void setUp() throws Exception
    {
        converter = new ActiveObjectsTableNameConverter(mockPrefix());
    }

    @Test
    public void testGetNameForSimpleEntity() throws Exception
    {
        assertEquals(PREFIX + "_" + "SIMPLE_ENTITY", converter.getName(SimpleEntity.class));
    }

    @Test
    public void testGetNameForAnnotatedEntity() throws Exception
    {
        assertEquals(PREFIX + "_" + "SIMPLE_ENTITY", converter.getName(AnnotatedEntity.class));
    }

    private static interface SimpleEntity extends RawEntity<Object>
    {
    }

    @Table("SimpleEntity")
    private static interface AnnotatedEntity extends RawEntity<Object>
    {
    }

    private static Prefix mockPrefix()
    {
        return new Prefix()
        {
            @Override
            public String prepend(String string)
            {
                return PREFIX + "_" + string;
            }

            @Override
            public boolean isStarting(String string, boolean caseSensitive)
            {
                return false;
            }
        };
    }
}
