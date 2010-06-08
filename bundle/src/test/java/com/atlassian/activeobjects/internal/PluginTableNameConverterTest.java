package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.TransactionCallback;
import com.atlassian.activeobjects.external.TransactionStatus;
import net.java.ao.DatabaseProvider;
import net.java.ao.Entity;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.schema.TableNameConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PluginTableNameConverterTest
{
    private static final String PLUGIN_KEY = "a.test.plugin.key";
    private static final String PLUGIN_SHORT_HASH = "13fba"; // This should be the first 5 chars of the SHA1 hash
    private static final String TABLE_SUFFIX = "entity";
    private static final String TABLE_NAME = "ao_"+ PLUGIN_SHORT_HASH +"_"+ TABLE_SUFFIX;

    private PluginTableNameConverter pluginTableNameConverter;

    @Before
    public void setUp()
    {
        pluginTableNameConverter = new PluginTableNameConverter(PLUGIN_KEY);
    }

    @Test
    public void testGetName() throws Exception
    {
        String name = pluginTableNameConverter.convertName(Entity.class);
        assertEquals(name, TABLE_NAME);
    }

    @Test
    public void testTooLongEntityName() throws Exception
    {
        try {
            pluginTableNameConverter.convertName(MyVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongEntityName.class);
            fail();
        } catch (EntityNameTooLongException e) {
            // Check that the entity can't be more than 21 characters
            assertTrue( e.getMessage().contains("21 char") );
        }
    }

    // Inner Classes

    public interface MyVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongEntityName extends Entity {

    }
}