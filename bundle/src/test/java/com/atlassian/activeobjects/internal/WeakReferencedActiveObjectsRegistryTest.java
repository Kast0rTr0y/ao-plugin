package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.config.ActiveObjectsConfiguration;
import com.atlassian.activeobjects.config.internal.ConfigurationUpdatedPredicate;
import com.atlassian.activeobjects.external.ActiveObjects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

/**
 * Testing {@link com.atlassian.activeobjects.internal.WeakReferencedActiveObjectsRegistry}
 */
@RunWith(MockitoJUnitRunner.class)
public final class WeakReferencedActiveObjectsRegistryTest
{
    private WeakReferencedActiveObjectsRegistry registry;

    private ActiveObjectsConfiguration configuration;

    @Before
    public void setUp() throws Exception
    {
        registry = new WeakReferencedActiveObjectsRegistry();
    }

    @Test
    public void testGet() throws Exception
    {
        assertNull(registry.get(configuration));
    }

    @Test
    public void testRegister() throws Exception
    {
        final ActiveObjects ao = mock(ActiveObjects.class);

        assertNull(registry.get(configuration));
        assertEquals(ao, registry.register(configuration, ao));
        assertEquals(ao, registry.get(configuration));
    }

    @Test
    public void testOnDirectoryUpdated() throws Exception
    {
        final ActiveObjectsConfiguration configuration2 = mock(ActiveObjectsConfiguration.class);
        final ActiveObjects ao1 = mock(ActiveObjects.class);
        final ActiveObjects ao2 = mock(ActiveObjects.class);

        registry.register(configuration, ao1);
        registry.register(configuration2, ao2);

        assertEquals(ao1, registry.get(configuration));
        assertEquals(ao2, registry.get(configuration2));

        registry.onConfigurationUpdated(new ConfigurationUpdatedPredicate()
        {
            public boolean matches(ActiveObjects activeObjects, ActiveObjectsConfiguration configuration)
            {
                return activeObjects == ao2;
            }
        });

        assertEquals(ao1, registry.get(configuration));
        assertNull(registry.get(configuration2));
    }
}
