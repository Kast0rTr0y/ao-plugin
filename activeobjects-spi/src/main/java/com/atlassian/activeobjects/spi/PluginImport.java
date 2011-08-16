package com.atlassian.activeobjects.spi;

import java.io.InputStream;

/**
 * This interface allows the {@link Backup} to find the inputstream corresponding to a given AO plugin (info).
 */
public interface PluginImport extends BackupErrorListener
{
    /**
     * The plugin info of the {@link #getInputStream() backup} to import.
     *
     * @return the plugin information. This must at least contain a valid AO plugin hash as previously exported.
     */
    PluginInformation getPluginInformation();

    /**
     * The input stream to the actual backup
     *
     * @return an input stream
     */
    InputStream getInputStream();
}
