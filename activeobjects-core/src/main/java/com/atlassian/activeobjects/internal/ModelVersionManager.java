package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ModelVersion;

public interface ModelVersionManager
{
    ModelVersion getCurrent(Prefix tableNamePrefix);

    void update(Prefix tableNamePrefix, ModelVersion version);
}
