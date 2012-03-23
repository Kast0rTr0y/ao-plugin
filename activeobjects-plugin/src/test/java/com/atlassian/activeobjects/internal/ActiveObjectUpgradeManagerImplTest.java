package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public final class ActiveObjectUpgradeManagerImplTest
{
    private ActiveObjectUpgradeManagerImpl upgradeManager;

    @Mock
    private ModelVersionManager modelVersionManager;

    @Before
    public void setUp()
    {
        upgradeManager = new ActiveObjectUpgradeManagerImpl(modelVersionManager);
    }

    @Test(expected = IllegalStateException.class)
    public void twoUpgradeTasksWithSameModelVersionThrowsException()
    {
        final List<ActiveObjectsUpgradeTask> upgradeTasks = Lists.<ActiveObjectsUpgradeTask>newArrayList(
                new MockActiveObjectsUpgradeTask(ModelVersion.valueOf("1")),
                new MockActiveObjectsUpgradeTask(ModelVersion.valueOf("1")),
                new MockActiveObjectsUpgradeTask(ModelVersion.valueOf("2"))
        );
        upgradeManager.verify(upgradeTasks);
    }

    @Test
    public void sortedUpgradeTasksWithDifferentModelVersionDoesNotThrowException()
    {
        final List<ActiveObjectsUpgradeTask> upgradeTasks = Lists.<ActiveObjectsUpgradeTask>newArrayList(
                new MockActiveObjectsUpgradeTask(ModelVersion.valueOf("1")),
                new MockActiveObjectsUpgradeTask(ModelVersion.valueOf("3")),
                new MockActiveObjectsUpgradeTask(ModelVersion.valueOf("4"))
        );
        upgradeManager.verify(upgradeTasks);
    }

    private static final class MockActiveObjectsUpgradeTask implements ActiveObjectsUpgradeTask
    {
        private final ModelVersion version;

        public MockActiveObjectsUpgradeTask(ModelVersion version)
        {
            this.version = version;
        }

        @Override
        public ModelVersion getModelVersion()
        {
            return version;
        }

        @Override
        public void upgrade(ModelVersion currentVersion, ActiveObjects ao)
        {
        }
    }
}
