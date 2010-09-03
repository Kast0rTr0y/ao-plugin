package com.atlassian.activeobjects.util;

import static java.lang.Math.max;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

/**
 * <p>A digester that creates an hexadecimal representation of the md5 sum of the String it applies to.</p>
 * <p>Note: the String is considered {@code UTF-8} encoded for any tranformation to and from {@link byte bytes}.</p>
 */
public final class HexMd5Digester implements Digester
{
    public String digest(String s)
    {
        return md5Hex(s);
    }

    public String digest(String s, int n)
    {
        return getLastNCharacters(digest(s), n);
    }

    String getLastNCharacters(String digested, int n)
    {
        return digested.substring(max(0, digested.length() - n));
    }
}
