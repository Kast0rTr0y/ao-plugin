package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ForeignKey;

public interface ForeignKeyCreator
{
    void create(Iterable<ForeignKey> foreignKeys, EntityNameProcessor entityNameProcessor);
}
