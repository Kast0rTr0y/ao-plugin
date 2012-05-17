package com.atlassian.activeobjects.spi;

public final class NullRestoreProgressMonitor extends AbstractRestoreProgressMonitor
{
    public static final RestoreProgressMonitor INSTANCE = new NullRestoreProgressMonitor();

    private NullRestoreProgressMonitor()
    {
    }
}
