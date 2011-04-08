package com.atlassian.dbexporter.progress;


/**
 * Implement this interface to be notified of progress of long running tasks.
 * @since 0.9.7
 */
public interface ProgressMonitor
{
    void begin(Object... args);

    void end(Object... args);

    void begin(Task task, Object... args);

    void end(Task task, Object... args);

    void totalNumberOfTables(int size);

    static enum Task
    {
        /** This is the task for importing/exporting the database information */
        DATABASE_INFORMATION,

        /** This is the task for importing/exporting the table definitions */
        TABLE_DEFINITION,

        /** This is the task of creating a table */
        TABLE_CREATION,

        /** This is the task of importing/exporting <strong>all</strong> tables data */
        TABLES_DATA,

        /** This is the task of importing/exporting a <strong>single</strong> table data */
        TABLE_DATA,

        TABLE_ROW,
    }
}
