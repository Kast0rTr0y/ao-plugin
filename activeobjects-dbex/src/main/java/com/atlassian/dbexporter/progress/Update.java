package com.atlassian.dbexporter.progress;

/** Subtype that indicates a normal status update. */
public final class Update extends Message
{
    public Update(String message)
    {
        super(message);
    }

    public static Update from(String msg)
    {
        return new Update(msg);
    }

    public static Update from(String msg, Object... objects)
    {
        return from(String.format(msg, objects));
    }
}
