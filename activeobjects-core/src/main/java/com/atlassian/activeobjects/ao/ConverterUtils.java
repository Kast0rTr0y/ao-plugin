package com.atlassian.activeobjects.ao;

import net.java.ao.ActiveObjectsException;

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

    public static String checkLength(String name, String errorMsg)
    {
        if (name != null && name.length() > MAX_LENGTH)
        {
            throw new ActiveObjectsException(errorMsg);
        }
        return name;
    }
}
