package com.atlassian.activeobjects.spi;

public final class NullBackupProgressMonitor extends AbstractBackupProgressMonitor
{
    public static final BackupProgressMonitor INSTANCE = new NullBackupProgressMonitor();

    private NullBackupProgressMonitor()
    {
    }
}
