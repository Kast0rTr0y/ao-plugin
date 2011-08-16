package com.atlassian.activeobjects.spi;

public interface BackupErrorListener
{
    /**
     * This method is called on error when importing or exporting a plugin related database
     *
     * @param information the known information about the plugin. This can be as little as the {@code hash} used by AO
     * for this plugin.
     * @param t the exception that has been thrown while importing/exporting
     * @return whether the process of importing/exporting should skip this particular plugin or continue when an error occurs.
     */
    OnBackupError error(PluginInformation information, Throwable t);
}
