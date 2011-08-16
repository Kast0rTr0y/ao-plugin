package com.atlassian.activeobjects.spi;

import java.util.List;

/**
 * Makes backup/restore possible ;-)
 */
public interface Backup
{
    /**
     * This is the method that the application will call when doing the backup.
     *
     * @param export the callback to save the output streams associated with plugins. Output streams used by the export
     * will be closed as the export progresses.
     * @param monitor the progress monitor for the current backup
     * @return the list of errors that happened during export
     */
    List<BackupError> save(PluginExport export, BackupProgressMonitor monitor);

    /**
     * <p>This is the method that the application will call when restoring data.</p>
     *
     * @param imports the list of streams to import as Active Objects backups. Provided input streams will automatically
     * be closed as soon as each restoring is complete.
     * @param monitor the progress monitor for the current restore
     * @return the list of errors that happened during import
     */
    List<BackupError> restore(Iterable<PluginImport> imports, RestoreProgressMonitor monitor);
}