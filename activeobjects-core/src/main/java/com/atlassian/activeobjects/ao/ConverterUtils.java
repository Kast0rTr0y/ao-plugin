package com.atlassian.activeobjects.ao;

import java.util.Locale;

public final class ConverterUtils
{
    public static final int MAX_LENGTH = 30;

    private ConverterUtils()
    {
    }

    public static String toUpperCase(String name)
    {
        return name == null ? name : name.toUpperCase(Locale.ENGLISH);
    }
}
