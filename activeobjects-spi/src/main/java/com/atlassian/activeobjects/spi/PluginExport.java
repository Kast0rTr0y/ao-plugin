package com.atlassian.activeobjects.spi;

import java.io.OutputStream;

/**
 * This interface allows the {@link Backup} to query for the output stream for a given AO (plugin) backup to happen.
 */
public interface PluginExport extends BackupErrorListener
{
    /**
     * The output stream to use for exporting the backup for a given plugin (info)
     *
     * @param info the info about the plugin. This can be as little as the plugin AO hash, but no less. {@see PluginInformation}
     * @return an output stream.
     */
    OutputStream getOutputStream(PluginInformation info);
}
