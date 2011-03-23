package com.atlassian.activeobjects.external;

public interface ActiveObjectsUpgradeTask
{
    ModelVersion getModelVersion();

    void upgrade(ModelVersion currentVersion, ActiveObjects ao);
}
