package com.atlassian.activeobjects.spi;

/**
 * Represents a backup error, this is valid for both exporting or importing
 */
public interface BackupError
{
    /**
     * The information about the plugin that made this error happen.
     */
    PluginInformation getPluginInformation();

    /**
     * The actual exception that happened
     */
    Throwable getThrowable();
}
