package com.atlassian.activeobjects.backup;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ActiveObjectsHashesReaderTest
{
    private ActiveObjectsHashesReader reader;

    @Before
    public final void setUp()
    {
        reader = new ActiveObjectsHashesReader();
    }

    @Test
    public void testGetHashForValidTableNames() throws Exception
    {
        assertEquals("HASH", reader.getHash("AO_HASH_TABLE_NAME"));
        assertEquals("HASH", reader.getHash("AO_HASH_TABLENAME"));
    }

    @Test
    public void testGetHashForAoTableNameWithNoHash() throws Exception
    {
        assertEquals("", reader.getHash("AO_TABLENAME"));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetHashForNonAoTableName() throws Exception
    {
        assertEquals("", reader.getHash("HASH_TABLENAME"));
    }
}
