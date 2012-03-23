package com.atlassian.dbexporter;

public enum BatchMode
{
    ON, OFF;

    static BatchMode from(boolean batch)
    {
        return batch ? ON : OFF;
    }
}
