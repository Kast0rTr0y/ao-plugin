package com.atlassian.dbexporter;

import java.util.Map;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Maps.*;

public final class DatabaseInformation
{
    private final Map<String, String> meta;

    public DatabaseInformation(Map<String, String> meta)
    {
        this.meta = newHashMap(checkNotNull(meta));
    }

    public <T> T get(String key, StringConverter<T> converter)
    {
        return converter.convert(getString(key));
    }

    public <T> T get(String key, StringConverter<T> converter, T defaultValue)
    {
        return converter.convert(getString(key), defaultValue);
    }

    public String getString(String key)
    {
        return meta.get(key);
    }

    public String getString(String key, String defaultValue)
    {
        return get(key, new StringStringConverter(), defaultValue);
    }

    public int getInt(String key)
    {
        return get(key, new IntStringConverter());
    }

    public int getInt(String key, int defaultValue)
    {
        return get(key, new IntStringConverter(), defaultValue);
    }

    public boolean isEmpty()
    {
        return meta.isEmpty();
    }

    public static interface StringConverter<T>
    {
        T convert(String s);

        T convert(String s, T defaultValue);
    }

    public static abstract class AbstractStringConverter<T> implements StringConverter<T>
    {
        public final T convert(String s, T defaultValue)
        {
            final T value = convert(s);
            return value != null ? value : defaultValue;
        }
    }

    private static final class StringStringConverter extends AbstractStringConverter<String>
    {
        public String convert(String s)
        {
            return s;
        }
    }

    private static final class IntStringConverter extends AbstractStringConverter<Integer>
    {
        public Integer convert(String s)
        {
            return Integer.valueOf(s);
        }
    }
}
