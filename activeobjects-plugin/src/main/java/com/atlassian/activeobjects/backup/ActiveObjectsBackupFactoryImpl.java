package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.DatabaseProvider;

import static com.google.common.base.Preconditions.*;

public final class ActiveObjectsBackupFactoryImpl implements ActiveObjectsBackupFactory
{
    @Override
    public ActiveObjectsBackup newAoBackup(DatabaseProvider databaseProvider, PrefixedSchemaConfigurationFactory schemaConfigurationFactory, Prefix prefix)
    {
        return new PrefixedActiveObjectsBackup(databaseProvider, schemaConfigurationFactory, checkNotNull(prefix));
    }
}
