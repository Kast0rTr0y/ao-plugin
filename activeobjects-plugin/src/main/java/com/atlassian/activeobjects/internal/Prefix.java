package com.atlassian.activeobjects.internal;

/**
 * Represents a String's prefix
 */
public interface Prefix
{
    /**
     * Prepends that prefix to the given String
     *
     * @param string the String to prepend the prefix to.
     * @return the new String with the prefix prepended
     */
    String prepend(String string);

    /**
     * Tells whether the prefix is at the start of the given String
     *
     * @param string checks whether {@code this} starts the String
     * @param caseSensitive whether or not we're case sensitive
     * @return {@code true} if the string starts with the given prefix.
     */
    boolean isStarting(String string, boolean caseSensitive);
}
