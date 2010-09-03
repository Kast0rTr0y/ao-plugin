package com.atlassian.activeobjects.internal.backup;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.backup.Backup;
import org.osgi.framework.Bundle;

public interface ActiveObjectsBackupFactory
{
    Backup getBackup(Bundle bundle, ActiveObjects ao);
}
