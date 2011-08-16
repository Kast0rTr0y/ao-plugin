package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.spi.BackupProgressMonitor;
import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.activeobjects.spi.RestoreProgressMonitor;

import java.io.InputStream;
import java.io.OutputStream;

interface ActiveObjectsBackup
{
    void save(OutputStream stream, BackupProgressMonitor monitor, PluginInformation pluginInfo);

    void restore(InputStream stream, RestoreProgressMonitor monitor, PluginInformationChecker pluginInformationChecker);
}
