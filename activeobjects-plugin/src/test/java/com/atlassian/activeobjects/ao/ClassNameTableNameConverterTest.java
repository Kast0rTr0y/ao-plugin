package com.atlassian.activeobjects.ao;

import net.java.ao.Entity;
import net.java.ao.schema.TableNameConverter;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ClassNameTableNameConverterTest
{
    final TableNameConverter converter = new ClassNameTableNameConverter();

    @Test
    public void someClass()
    {
        assertEquals("MyEntity", converter.getName(MyEntity.class));
    }

    private static interface MyEntity extends Entity
    {
    }
}
