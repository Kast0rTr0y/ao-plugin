package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.ao.ActiveObjectsTableNameConverter;
import com.atlassian.activeobjects.internal.Prefix;
import com.atlassian.activeobjects.internal.SimplePrefix;
import net.java.ao.RawEntity;
import net.java.ao.schema.TableNameConverter;

import static com.atlassian.activeobjects.plugin.ActiveObjectModuleDescriptor.AO_TABLE_PREFIX;

public final class BackupActiveObjectsTableNameConverter implements TableNameConverter
{
    public static final Prefix PREFIX = new SimplePrefix(AO_TABLE_PREFIX);

    private final TableNameConverter tableNameConverter;

    public BackupActiveObjectsTableNameConverter()
    {
        tableNameConverter = new ActiveObjectsTableNameConverter(PREFIX);
    }

    @Override
    public String getName(Class<? extends RawEntity<?>> clazz)
    {
        return tableNameConverter.getName(clazz);
    }
}
