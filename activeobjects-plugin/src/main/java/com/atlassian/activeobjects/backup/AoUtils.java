package com.atlassian.activeobjects.backup;

/**
 * This is simply copied from AO
 */
class AoUtils
{
    private static final int MAX_LENGTH = 30;

    public static String shorten(String id)
    {
        final int maxIDLength = MAX_LENGTH;
        if (id.length() > maxIDLength)
        {
            int tailLength = maxIDLength / 3;
            int hash = (int) (id.hashCode() % Math.round(Math.pow(10, tailLength)));
            hash = Math.abs(hash);

            id = id.substring(0, maxIDLength - tailLength - 1);
            id += hash;
        }
        return id;
    }
}
