package com.atlassian.dbexporter.node;

import com.atlassian.dbexporter.ImportExportException;

public final class ParseException extends ImportExportException
{
    public ParseException()
    {
        super();
    }

    public ParseException(String message)
    {
        super(message);
    }

    public ParseException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ParseException(Throwable cause)
    {
        super(cause);
    }
}