package com.atlassian.activeobjects.internal.backup;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.backup.Backup;
import org.osgi.framework.Bundle;

public final class ActiveObjectsBackupFactoryImpl implements ActiveObjectsBackupFactory
{
    public Backup getBackup(final Bundle bundle, ActiveObjects ao)
    {
        return new ActiveObjectsBackup(bundle, ao);
    }
}
