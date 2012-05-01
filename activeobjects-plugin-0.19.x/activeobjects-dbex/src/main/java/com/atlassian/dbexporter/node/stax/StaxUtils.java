package com.atlassian.dbexporter.node.stax;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Maps.*;

final class StaxUtils
{
    private static final String WOODSTOX_INPUT_FACTORY = "com.ctc.wstx.stax.WstxInputFactory";
    private static final String DEFAULT_INPUT_FACTORY = "com.sun.xml.internal.stream.XMLInputFactoryImpl";
    private static final String WOODSTOX_OUTPUT_FACTORY = "com.ctc.wstx.stax.WstxOutputFactory";
    private static final String DEFAULT_OUTPUT_FACTORY = "com.sun.xml.internal.stream.XMLOutputFactoryImpl";

    private static final char BACKSLASH = '\\';
    private static final Map<Character, String> CHAR_TO_UNICODE;

    static
    {
        final String escapeString = "\u0000\u0001\u0002\u0003\u0004\u0005" +
                "\u0006\u0007\u0008\u000B\u000C\u000E\u000F\u0010\u0011\u0012" +
                "\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C" +
                "\u001D\u001E\u001F\uFFFE\uFFFF";

        CHAR_TO_UNICODE = newHashMap();

        for (int i = 0; i < escapeString.length(); i++)
        {
            final char c = escapeString.charAt(i);
            CHAR_TO_UNICODE.put(c, String.format("\\u%04X", (int) c));
        }
        CHAR_TO_UNICODE.put('\\', "\\\\");
    }

    /**
     * Replaces all characters that are illegal in XML with a Java-like unicode
     * escape sequence '\\u[0-9][0-9][0-9][0-9]'. When <code>null</code> is
     * passed into this method, <code>null</code> is returned.
     *
     * @see #unicodeDecode(String)
     */
    public static String unicodeEncode(String string)
    {

        if (string == null)
        {
            return null;
        }
        else
        {
            final StringBuilder copy = new StringBuilder();
            copy.setLength(0);
            boolean copied = false;

            for (int i = 0; i < string.length(); i++)
            {

                char c = string.charAt(i);
                String s = CHAR_TO_UNICODE.get(c);
                if (s != null && !copied)
                {
                    copy.append(string.substring(0, i));
                    copied = true;
                }
                if (copied)
                {
                    if (s == null)
                    {
                        copy.append(c);
                    }
                    else
                    {
                        copy.append(s);
                    }
                }
            }
            return copied ? copy.toString() : string;
        }
    }

    /**
     * Substitutes all occurances of '\\u[0-9][0-9][0-9][0-9]' with their
     * corresponding character codes. When <code>null</code> is passed into this
     * method, <code>null</code> is returned.
     *
     * @see #unicodeEncode(String)
     */
    public static String unicodeDecode(String string)
    {

        if (string == null)
        {
            return null;
        }
        else
        {
            final StringBuilder copy = new StringBuilder();
            copy.setLength(0);
            boolean copied = false;

            for (int i = 0; i < string.length(); i++)
            {
                char c = string.charAt(i);
                if (c == BACKSLASH)
                {
                    if (!copied)
                    {
                        copy.append(string.substring(0, i));
                        copied = true;
                    }
                    if (string.charAt(++i) == BACKSLASH)
                    {
                        copy.append(BACKSLASH);
                    }
                    else
                    {
                        // must be a unicode string
                        String value = string.substring(++i, i + 4);
                        copy.append((char) Integer.parseInt(value, 16));
                        i += 3;
                    }
                }
                else if (copied)
                {
                    copy.append(c);
                }
            }
            return copied ? copy.toString() : string;
        }
    }

    public static DateFormat newDateFormat()
    {
        final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    static XMLInputFactory newXmlInputFactory()
    {
        return newInstance(XMLInputFactory.class, WOODSTOX_INPUT_FACTORY, DEFAULT_INPUT_FACTORY);
    }

    static XMLOutputFactory newXmlOutputFactory()
    {
        return newInstance(XMLOutputFactory.class, WOODSTOX_OUTPUT_FACTORY, DEFAULT_OUTPUT_FACTORY);
    }

    private static <T> T newInstance(Class<T> type, String... classNames)
    {
        final List<XmlFactoryException> exceptions = newArrayList();
        for (String className : classNames)
        {
            try
            {
                return newInstance(type, className);
            }
            catch (XmlFactoryException e)
            {
                exceptions.add(e);
            }
        }
        throw new XmlFactoryException("Could not instantiate any of " + Arrays.toString(classNames), exceptions.toArray(new Throwable[exceptions.size()]));
    }

    private static <T> T newInstance(Class<T> type, String className)
    {
        try
        {
            return type.cast(StaxUtils.class.getClassLoader().loadClass(className).newInstance());
        }
        catch (InstantiationException e)
        {
            throw new XmlFactoryException("Could not instantiate " + className, e);
        }
        catch (IllegalAccessException e)
        {
            throw new XmlFactoryException("Could not instantiate " + className, e);
        }
        catch (ClassNotFoundException e)
        {
            throw new XmlFactoryException("Could not find class " + className, e);
        }
    }
}
