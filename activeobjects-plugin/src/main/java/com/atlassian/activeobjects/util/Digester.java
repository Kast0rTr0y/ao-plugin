package com.atlassian.activeobjects.util;

/**
 * A simple interface to get digests of any {@link String}
 */
public interface Digester
{
    /**
     * Digests the String into another String
     *
     * @param s the String to digest
     * @return the digested String
     * @see #digest(String, int)
     */
    String digest(String s);

    /**
     * Digest the String into another String, keeping at most the last {@code n} characters of the digested String
     *
     * @param s the String to digest
     * @param n the number of 'last' characters to keep
     * @return the digested String
     */
    String digest(String s, int n);
}
