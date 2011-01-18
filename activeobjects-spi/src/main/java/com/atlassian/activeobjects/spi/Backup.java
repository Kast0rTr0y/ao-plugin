package com.atlassian.activeobjects.spi;

import java.io.InputStream;
import java.io.OutputStream;

/** Makes backup/restore possible ;-) */
public interface Backup
{
    /**
     * This is the method that the application will call when doing the backup.
     *
     * @param os the stream to write the backup to
     */
    void save(OutputStream os);

    /**
     * <p>This is the method that the application will call when restoring data.</p>
     *
     * @param stream the stream of data previously backed up by the plugin.
     */
    void restore(InputStream stream);
}