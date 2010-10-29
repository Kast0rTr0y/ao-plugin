package com.atlassian.activeobjects.osgi;

/**
 * Exception thrown when too many services were found. Typically if you're looking for
 * only one service and 2 or more exist, this exception should be thrown.
 */
public class TooManyServicesFoundException extends Exception
{
    public TooManyServicesFoundException(String msg)
    {
        super(msg);
    }
}
