package com.atlassian.dbexporter.progress;

/**
 * Implement this interface to be notified of progress of long running tasks.
 *
 * @author Erik van Zijst
 */
public interface ProgressMonitor
{
    /**
     * @param resource object provided by the resource that is being monitored.
     * Use this to obtain information about the progress.
     */
    void update(Message resource);
}
