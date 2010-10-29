package com.atlassian.activeobjects.util;

/**
 * A set of utils functions. Those mainly come from Google Collections or Commons Lang. It might be wise to replace usage with those
 * when/if this class becomes too big.
 */
public final class ActiveObjectsUtils
{
    ///CLOVER:OFF
    private ActiveObjectsUtils()
    {
    }
    ///CLOVER:ON

    /**
     * Check whether the parameter is {@code null}
     * @param t the parameter that might be {@code null}
     * @param <T> the type of the parameter
     * @return the unmodified parameter
     * @throws IllegalArgumentException if the parameter is {@code null}.
     */
    public static <T> T checkNotNull(T t)
    {
        ///CLOVER:OFF
        if (t == null)
        {
            throw new IllegalArgumentException("Parameter must not be null");
        }
        ///CLOVER:ON
        return t;
    }
}
