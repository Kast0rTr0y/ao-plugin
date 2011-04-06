package com.atlassian.activeobjects.external;

/**
 * This is the interface to implement for writing upgrade tasks.
 */
public interface ActiveObjectsUpgradeTask
{
    /**
     * This gives the version the upgrade task will upgrade the model to. This is this version that is used by the
     * Active Objects plugin to figure out whether this upgrade task should be run and in which order.
     *
     * @return the version of the model after the upgrade task has been run successfully.
     */
    ModelVersion getModelVersion();

    /**
     * Upgrades the database model of this Active Objects plugin to the {@link #getModelVersion() given version}.
     *
     * @param currentVersion the current version of the model currently in the database.
     * @param ao a configured instance of the Active Objects, which is not associated with any entity (yet).
     */
    void upgrade(ModelVersion currentVersion, ActiveObjects ao);
}
