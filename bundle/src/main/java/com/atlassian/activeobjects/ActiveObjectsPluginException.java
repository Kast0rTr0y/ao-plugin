package com.atlassian.activeobjects;

/**
 * A generic runtime exception for all active objects linked exceptions.
 */
///CLOVER:OFF
public class ActiveObjectsPluginException extends RuntimeException
{
    public ActiveObjectsPluginException()
    {
    }

    public ActiveObjectsPluginException(String message)
    {
        super(message);
    }

    public ActiveObjectsPluginException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ActiveObjectsPluginException(Throwable cause)
    {
        super(cause);
    }
}
