package com.atlassian.dbexporter.node.stax;

import org.junit.Test;

import static org.junit.Assert.*;

public class StaxUtilsTest
{
    private final String legal = "legal: \u0020 \uFFFC";
    private final String decoded = "Illegal: \u0000 \u0008 \u0010 \u000B \u001F \uFFFE \\";
    private final String encoded = "Illegal: \\u0000 \\u0008 \\u0010 \\u000B \\u001F \\uFFFE \\\\";

    @Test
    public void testUnicodeDecoding() throws Exception
    {
        assertEquals(legal, StaxUtils.unicodeDecode(legal));
        assertEquals(decoded, StaxUtils.unicodeDecode(encoded));
        assertNull(StaxUtils.unicodeDecode(null));
    }

    @Test
    public void testUnicodeEncoding() throws Exception
    {
        assertEquals(legal, StaxUtils.unicodeEncode(legal));
        assertEquals(encoded, StaxUtils.unicodeEncode(decoded));
        assertNull(StaxUtils.unicodeEncode(null));
    }

    /**
     * This test pushes a string that contains every valid 16-bit Java char
     * through the unicode encoder and decoder and verifies that the exact string
     * is properly restored.
     *
     * @throws Exception
     */
    @Test
    public void testCodec() throws Exception
    {
        final String encoded = StaxUtils.unicodeEncode(createTestString());
        final String decoded = StaxUtils.unicodeDecode(encoded);

        assertEquals(createTestString(), decoded);
    }

    /**
     * Returns a unicode string that contains every character from U+0000 to
     * U+FFFF (AKA the Basic Multilingual Plane (BMP)).
     */
    private String createTestString()
    {
        StringBuilder builder = new StringBuilder();
        for (char c = 0x0; c < 0xFFFF; c++)
        {
            if (c >= 0xd801 && c <= 0xdbff)
            {
                continue;   // exclude the high surrogate range
            }
            else
            {
                builder.append(c);
            }
        }
        return builder.append((char) 0xFFFF).toString();
    }
}
