package com.atlassian.activeobjects.backup;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.ForeignKey;

public interface ForeignKeyCreator
{
    void create(Iterable<ForeignKey> foreignKeys, Context context);
}
