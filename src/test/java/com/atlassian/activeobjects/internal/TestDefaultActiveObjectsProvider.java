package com.atlassian.activeobjects.internal;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import com.atlassian.plugin.test.PluginTestUtils;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.activeobjects.external.ActiveObjects;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.java.ao.Entity;

/**
 *
 */
public class TestDefaultActiveObjectsProvider extends TestCase
{
    private DefaultActiveObjectsProvider prov;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File dbdir = PluginTestUtils.createTempDirectory(getClass());
        dbdir.mkdir();

        ApplicationProperties appProps = mock(ApplicationProperties.class);
        when(appProps.getHomeDirectory()).thenReturn(dbdir);
        prov = new DefaultActiveObjectsProvider(appProps, new DefaultActiveObjectsConfiguration());
    }

    public void testGetEntityManager() throws IOException, SQLException
    {
        ActiveObjects mgr = prov.createActiveObjects("foo");
        mgr.migrate(Person.class);

        Person bob = mgr.create(Person.class);
        bob.setName("bob");
        bob.save();

        mgr = prov.createActiveObjects("foo");
        bob = mgr.find(Person.class)[0];
        assertEquals("bob", bob.getName());

    }

    public void testGetEntityManagerMultipleGets() throws IOException, SQLException
    {
        ActiveObjects mgr1 = prov.createActiveObjects("foo");
        mgr1.migrate(Person.class);
        ActiveObjects mgr2 = prov.createActiveObjects("foo");

        Person bob = mgr1.create(Person.class);
        bob.setName("bob");
        bob.save();

        bob = mgr2.find(Person.class)[0];
        assertEquals("bob", bob.getName());
    }

    public void testGetEntityManagerAutocleanup() throws IOException, SQLException
    {
        ActiveObjects mgr = prov.createActiveObjects("foo");
        mgr.migrate(Person.class);

        Person bob = mgr.create(Person.class);
        bob.setName("bob");
        bob.save();

        int id = System.identityHashCode(mgr);
        mgr = null;
        for (int x=0; x<10; x++)
        {
            System.gc();
        }

        ActiveObjects mgr2 = prov.createActiveObjects("foo");
        assertTrue(id != System.identityHashCode(mgr2));
        bob = mgr2.find(Person.class)[0];
        assertEquals("bob", bob.getName());
    }

    public interface Person extends Entity
    {
        public String getName();
        public void setName(String name);
    }
}
