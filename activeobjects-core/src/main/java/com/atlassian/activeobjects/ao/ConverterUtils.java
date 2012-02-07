package com.atlassian.activeobjects.ao;

import java.util.Locale;

public final class ConverterUtils
{
    private ConverterUtils()
    {
    }

    public static String toLowerCase(String s)
    {
        return s == null ? s : s.toLowerCase(Locale.ENGLISH);
    }
    
    public static String toUpperCase(String s)
    {
        return s == null ? s : s.toUpperCase(Locale.ENGLISH);
    }
}
