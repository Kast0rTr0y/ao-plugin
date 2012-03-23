package com.atlassian.activeobjects.spi;

/**
 * The generic <em>runtime</em> exception for the DB exporter module.
 *
 * @since 1.0
 */
public abstract class ImportExportException extends RuntimeException
{
    public ImportExportException(String message)
    {
        super(message);
    }

    public ImportExportException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ImportExportException(Throwable cause)
    {
        super(cause);
    }
}
