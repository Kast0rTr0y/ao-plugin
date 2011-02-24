package com.atlassian.dbexporter.node.stax;

public final class XmlFactoryException extends RuntimeException
{
    private final Throwable first;
    private final Throwable second;

    public XmlFactoryException(String s, Throwable first, Throwable second)
    {
        super(s, second);
        this.first = first;
        this.second = second;
    }

    public Throwable getFirst()
    {
        return first;
    }

    public Throwable getSecond()
    {
        return second;
    }
}
