package com.atlassian.activeobjects.spi;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Makes backup/restore possible ;-)
 */
public interface Backup
{
    /**
     * <p>This must be a unique identifier for the plugin/backup. This will is used by products to store backups
     * in a <em>well known</em> location so that it can be fed back to plugins when restoring data.</p>
     *
     * @return a unique identifier for the plugin/backup.
     */
    String getId();

    /**
     * <p>Used to check wether a backup with the given id can be restored by this instance.</p>
     *
     * @param id the id that was saved with the backup.
     * @return {@code true} if the id identifies a stream that can be backed up. Note that this doesn't necessarily means that
     *         <code>id.equals(this.getId()) == true</code>
     * @see #getId()
     * @see #restore(String, InputStream)
     */
    boolean accept(String id);

    /**
     * This is the method that the application will call when doing the backup.
     *
     * @param os the stream to write the backup to
     */
    void save(OutputStream os);

    /**
     * <p>This is the method that the application will call when restoring data.</p>
     * <p>This method MUST not be called unless the {@link #accept(String)} method returns {@code true}
     * when called with the {@code id} associated with the {@code stream}</p>
     *
     * @param id the id of the backup you wish to restore
     * @param stream the stream of data previously backed up by the plugin.
     * @see #accept(String)
     */
    void restore(String id, InputStream stream);
}