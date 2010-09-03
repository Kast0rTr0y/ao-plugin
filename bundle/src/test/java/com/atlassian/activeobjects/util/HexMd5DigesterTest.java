package com.atlassian.activeobjects.util;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Testing {@link com.atlassian.activeobjects.util.HexMd5Digester}
 */
public class HexMd5DigesterTest
{
    private HexMd5Digester digester = new HexMd5Digester();

    @Test
    public void testGetLastNCharacters() throws Exception
    {
        final int n = 6;
        final String sixCharsLong = "sixsix";
        final String lessThanSixCharsLong = "six";

        assertEquals(sixCharsLong, digester.getLastNCharacters("some-long-string" + sixCharsLong, n));
        assertEquals(sixCharsLong, digester.getLastNCharacters(sixCharsLong, n));
        assertEquals(lessThanSixCharsLong, digester.getLastNCharacters(lessThanSixCharsLong, n));
    }
}
