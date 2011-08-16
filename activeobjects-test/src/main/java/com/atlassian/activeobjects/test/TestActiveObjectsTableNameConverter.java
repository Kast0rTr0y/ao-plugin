package com.atlassian.activeobjects.test;

import com.atlassian.activeobjects.ao.ActiveObjectsTableNameConverter;
import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.RawEntity;
import net.java.ao.schema.TableNameConverter;

public final class TestActiveObjectsTableNameConverter implements TableNameConverter
{
    private static final String PREFIX = "AO_";

    private final TableNameConverter tnc = new ActiveObjectsTableNameConverter(new Prefix()
    {
        @Override
        public String prepend(String string)
        {
            return PREFIX + string;
        }

        @Override
        public boolean isStarting(String string, boolean caseSensitive)
        {
            final String thePrefix = caseSensitive ? PREFIX : PREFIX.toLowerCase();
            final String toCompare = caseSensitive ? string : string.toLowerCase();
            return toCompare.startsWith(thePrefix);
        }
    });

    @Override
    public String getName(Class<? extends RawEntity<?>> clazz)
    {
        return tnc.getName(clazz);
    }
}
