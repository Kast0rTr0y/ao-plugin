package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjectsModuleMetaData;
import com.atlassian.activeobjects.spi.DatabaseType;

public abstract class AbstractActiveObjectsMetaData implements ActiveObjectsModuleMetaData {
    private final DatabaseType databaseType;

    protected AbstractActiveObjectsMetaData(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }
}
