package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.ao.AtlassianTablePrefix;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.SimplePrefix;
import net.java.ao.RawEntity;
import net.java.ao.atlassian.AtlassianTableNameConverter;
import net.java.ao.schema.TableNameConverter;

public final class BackupActiveObjectsTableNameConverter implements TableNameConverter
{
    public static final Prefix PREFIX = new SimplePrefix("AO_000000");

    private final TableNameConverter tableNameConverter;

    public BackupActiveObjectsTableNameConverter()
    {
        tableNameConverter = new AtlassianTableNameConverter(new AtlassianTablePrefix(PREFIX));
    }

    @Override
    public String getName(Class<? extends RawEntity<?>> clazz)
    {
        return tableNameConverter.getName(clazz);
    }
}
