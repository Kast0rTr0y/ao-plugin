package com.atlassian.dbexporter;

/**
 * The generic <em>runtime</em> exception for the DB exporter module.
 *
 * @since 1.0
 */
public class ImportExportException extends RuntimeException
{
    public ImportExportException()
    {
    }

    public ImportExportException(String message)
    {
        super(message);
    }

    public ImportExportException(String message, Throwable cause)
    {
        super(message, cause);
        initCause(cause);
    }

    public ImportExportException(Throwable cause)
    {
        super(cause);
        initCause(cause);
    }
}
