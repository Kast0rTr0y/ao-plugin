package com.atlassian.dbexporter.node.stax;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class XmlFactoryException extends RuntimeException
{
    private final List<Throwable> throwables;

    public XmlFactoryException(String s, Throwable... throwables)
    {
        super(s, throwables[throwables.length - 1]);
        this.throwables = ImmutableList.of(throwables);
    }

    public List<Throwable> getThrowables()
    {
        return throwables;
    }
}
