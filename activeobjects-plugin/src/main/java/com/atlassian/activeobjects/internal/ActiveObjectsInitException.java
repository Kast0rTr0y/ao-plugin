package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.ActiveObjectsPluginException;

/**
 * Exception thrown when an error occurs during initialization.
 * 
 */
public class ActiveObjectsInitException extends ActiveObjectsPluginException
{
    public ActiveObjectsInitException(String msg)
    {
        super(msg);
    }

    public ActiveObjectsInitException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
