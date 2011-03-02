package com.atlassian.activeobjects.backup;

public interface SequenceUpdater
{
    void update(String tableName, String columnName);
}
