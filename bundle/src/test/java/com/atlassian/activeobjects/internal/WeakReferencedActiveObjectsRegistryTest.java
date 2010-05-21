package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class WeakReferencedActiveObjectsRegistryTest
{
    private WeakReferencedActiveObjectsRegistry registry;

    @Before
    public void setUp() throws Exception
    {
        registry = new WeakReferencedActiveObjectsRegistry();
    }

    @Test
    public void testGet() throws Exception
    {
        assertNull(registry.get("a-not-so-random-key"));
    }

    @Test
    public void testRegister() throws Exception
    {
        final String key = "a-key";
        final ActiveObjects ao = mock(ActiveObjects.class);

        assertNull(registry.get(key));
        assertEquals(ao, registry.register(key, ao));
        assertEquals(ao, registry.get(key));
    }

    @Test
    public void testOnDirectoryUpdated() throws Exception
    {
        final String key1 = "key1";
        final String key2 = "key2";
        final ActiveObjects ao1 = mock(ActiveObjects.class);
        final ActiveObjects ao2 = mock(DatabaseDirectoryAwareActiveObjects.class);

        registry.register(key1, ao1);
        registry.register(key2, ao2);

        assertEquals(ao1, registry.get(key1));
        assertEquals(ao2, registry.get(key2));

        registry.onDirectoryUpdated();

        assertEquals(ao1, registry.get(key1));
        assertNull(registry.get(key2));
    }

    private static interface DatabaseDirectoryAwareActiveObjects extends ActiveObjects, DatabaseDirectoryAware
    {
    }
}
