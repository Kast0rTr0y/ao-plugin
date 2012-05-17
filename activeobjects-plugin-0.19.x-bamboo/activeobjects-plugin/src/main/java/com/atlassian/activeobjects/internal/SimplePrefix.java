package com.atlassian.activeobjects.internal;

import static com.atlassian.activeobjects.ao.ConverterUtils.toLowerCase;
import static com.google.common.base.Preconditions.*;

public final class SimplePrefix implements Prefix
{
    private static final String DEFAULT_SEPARATOR = "_";
    private final String prefix;
    private final String separator;

    public SimplePrefix(String prefix)
    {
        this(prefix, DEFAULT_SEPARATOR);
    }

    public SimplePrefix(String prefix, String separator)
    {
        this.prefix = checkNotNull(prefix);
        this.separator = checkNotNull(separator);
    }

    public String prepend(String string)
    {
        return new StringBuilder().append(prefix).append(separator).append(string).toString();
    }

    /**
     * Checks whether the string parameter starts with the prefix. This method is {@code null} safe and will return
     * {@code false} if the {@code string} parameter is {@code null}.
     * When checking case insensitively, the default locale will be used to lower case both the prefix and the
     * {@code string} parameter before comparing.
     *
     * @param string checks whether {@code this} starts the String
     * @param caseSensitive whether or not we're case sensitive
     * @return {@code true} of {@code string} starts with the prefix
     */
    public boolean isStarting(String string, boolean caseSensitive)
    {
        return string != null &&
                transform(string, caseSensitive).startsWith(transform(prefix + separator, caseSensitive));
    }

    private String transform(String s, boolean caseSensitive)
    {
        return caseSensitive ? s : toLowerCase(s);
    }
}
