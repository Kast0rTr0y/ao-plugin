package com.atlassian.activeobjects.osgi;

/**
 * Exception thrown when no service was found and at least one was expected
 */
public class NoServicesFoundException extends Exception
{
    public NoServicesFoundException(String msg)
    {
        super(msg);
    }
}
