package com.atlassian.dbexporter.progress;

/**
 * This type is used by the Crucible backup items to report status updates during
 * the backup and restore process. It has subtypes to distinguish between normal
 * messages and warnings.
 * <P>
 * Warnings are not immediately relayed to the backup client application, but
 * collected by {@link com.atlassian.crucible.migration.BackupManagerImpl} and displayed
 * at the end of the backup in warning report.
 * <P>
 * Warning messages are not used to report errors, where an error causes the
 * backup or restore to fail. Failures are "reported" by the {@link com.atlassian.crucible.migration.BackupItem}s
 * by throwing an exception.
 *
 * @author Erik van Zijst
 * @see ProgressMonitor
 */
public abstract class Message
{
    private final String message;

    protected Message(String message)
    {
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }

    public String toString()
    {
        return getMessage();
    }
}
