package com.atlassian.activeobjects.internal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ActiveObjectsConfigurationServiceListenerTest
{
    private ActiveObjectsConfigurationServiceListener listener;

    @Mock
    private DatabaseDirectoryListener databaseDirectoryListener;

    @Before
    public void setUp() throws Exception
    {
        listener = new ActiveObjectsConfigurationServiceListener(databaseDirectoryListener);
    }

    @Test
    public void testOnActiveObjectsConfigurationServiceUpdated() throws Exception
    {
        listener.onActiveObjectsConfigurationServiceUpdated(null);
        verify(databaseDirectoryListener).onDirectoryUpdated();
    }
}
