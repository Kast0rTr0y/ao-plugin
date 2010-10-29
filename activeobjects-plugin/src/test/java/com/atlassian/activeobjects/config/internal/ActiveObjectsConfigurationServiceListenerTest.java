package com.atlassian.activeobjects.config.internal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ActiveObjectsConfigurationServiceListenerTest
{
    private ActiveObjectsPluginConfigurationServiceListener listener;

    @Mock
    private ActiveObjectsConfigurationListener activeObjectsConfigurationListener;

    @Before
    public void setUp() throws Exception
    {
        listener = new ActiveObjectsPluginConfigurationServiceListener(activeObjectsConfigurationListener);
    }

    @Test
    public void testOnActiveObjectsConfigurationServiceUpdated() throws Exception
    {
        listener.onActiveObjectsConfigurationServiceUpdated(null);
        verify(activeObjectsConfigurationListener).onConfigurationUpdated(anyConfigurationUpdatedPredicate());
    }

    private ConfigurationUpdatedPredicate anyConfigurationUpdatedPredicate()
    {
        return Mockito.any();
    }
}
