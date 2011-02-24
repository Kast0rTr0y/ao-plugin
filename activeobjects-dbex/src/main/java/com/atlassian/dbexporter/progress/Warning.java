package com.atlassian.dbexporter.progress;

/**
 * Subtype that indicates a warning message.
 *
 * @see com.atlassian.crucible.migration.item.Update
 */
public final class Warning extends Message
{
    private Warning(String message)
    {
        super(message);
    }

    public static Warning from(String msg)
    {
        return new Warning(msg);
    }

    public static Warning from(String msg, Object... objects)
    {
        return from(String.format(msg, objects));
    }
}
