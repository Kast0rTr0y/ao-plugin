package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjectsModuleMetaData;
import com.atlassian.activeobjects.spi.DatabaseType;

public abstract class AbstractActiveObjectsMetaData implements ActiveObjectsModuleMetaData 
{
    private final Prefix tablePrefix;
    private final DatabaseType databaseType;

    protected AbstractActiveObjectsMetaData(Prefix tablePrefix, DatabaseType databaseType)
    {
        this.tablePrefix = tablePrefix;
        this.databaseType = databaseType;
    }

    public Prefix getTablePrefix()
    {
        return tablePrefix;
    }

    public DatabaseType getDatabaseType()
    {
        return databaseType;
    }

}
