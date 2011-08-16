package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.DatabaseProvider;

interface ActiveObjectsBackupFactory
{
    ActiveObjectsBackup newAoBackup(DatabaseProvider databaseProvider, PrefixedSchemaConfigurationFactory schemaConfigurationFactory, Prefix prefix);
}
