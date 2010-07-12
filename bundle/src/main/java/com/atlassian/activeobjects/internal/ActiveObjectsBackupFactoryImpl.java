package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.backup.Backup;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveObjectsBackupFactoryImpl implements ActiveObjectsBackupFactory
{
    private static final Logger log = LoggerFactory.getLogger(ActiveObjectsBackupFactoryImpl.class);

    public Backup getBackup(final Bundle bundle, ActiveObjects ao)
    {
        return new ActiveObjectsBackup(bundle, ao);
    }

}
