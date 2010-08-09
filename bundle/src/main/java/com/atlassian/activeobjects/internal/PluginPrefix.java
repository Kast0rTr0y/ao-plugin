package com.atlassian.activeobjects.internal;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.BundleContextAware;

import java.util.Locale;

import static com.atlassian.activeobjects.internal.util.ActiveObjectsUtils.checkNotNull;

/**
 * This represents a plugin prefix that can be used for example to prefix database table, or other plugin
 * specific data that shouldn't clash with other plugins.
 */
public final class PluginPrefix implements Prefix, BundleContextAware
{
    private static final char SEPARATOR = '_';

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private String prefix;

    public String prepend(String string)
    {
        return new StringBuilder().append(prefix).append(SEPARATOR).append(string).toString();
    }

    /**
     * Checks whether the string parameter starts with the prefix. This method is {@code null} safe and will return
     * {@code false} if the {@code string} parameter is {@code null}.
     * When checking case insensitively, the default locale will be used to lower case both the prefix and the
     * {@code string} parameter before comparing.
     *
     * @param string        checks whether {@code this} starts the String
     * @param caseSensitive whether or not we're case sensitive
     * @return {@code true} of {@code string} starts with the prefix
     */
    public boolean isStarting(String string, boolean caseSensitive)
    {
        return string != null &&
                transform(string, caseSensitive).startsWith(transform(prefix + SEPARATOR, caseSensitive));
    }

    private String transform(String s, boolean caseSensitive)
    {
        return caseSensitive ? s : s.toLowerCase(Locale.getDefault());
    }

    /**
     * The 'real' underlying prefix is calculated in that method, on setter injection. I really don't like that.
     * TODO consider using a factory bean to create the Prefix.
     *
     * @param bundleContext
     */
    public void setBundleContext(BundleContext bundleContext)
    {
        final String symbolicName = checkNotNull(bundleContext).getBundle().getSymbolicName();
        this.prefix = getPrefix(symbolicName);

        logger.debug("Database prefix for bundle <{}> is set to <{}>", symbolicName, prefix);
    }

    private static String getPrefix(String symbolicName)
    {
        return new StringBuilder()
                .append(getLastNCharacters(symbolicName, 4)) // getting the last N chars as this is what is most likely to be different
                .append(getLastNCharacters(Integer.toHexString(symbolicName.hashCode()), 4))
                .toString();
    }

    private static String getLastNCharacters(String symbolicName, int n)
    {
        return symbolicName.substring(Math.max(0, symbolicName.length() - n), symbolicName.length());
    }
}
