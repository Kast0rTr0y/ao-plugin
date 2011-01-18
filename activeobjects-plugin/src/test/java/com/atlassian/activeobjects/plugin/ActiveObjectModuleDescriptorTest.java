package com.atlassian.activeobjects.plugin;

import com.atlassian.activeobjects.internal.DataSourceTypeResolver;
import com.atlassian.activeobjects.osgi.OsgiServiceUtils;
import com.atlassian.activeobjects.util.Digester;
import com.atlassian.plugin.PluginException;
import com.google.common.collect.Sets;
import net.java.ao.Entity;
import net.java.ao.RawEntity;
import net.java.ao.schema.TableNameConverter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testing {@link com.atlassian.activeobjects.plugin.ActiveObjectModuleDescriptor}
 */
public final class ActiveObjectModuleDescriptorTest
{
    private ActiveObjectModuleDescriptor moduleDescriptor;

    @Before
    public void setUp()
    {
        moduleDescriptor = new ActiveObjectModuleDescriptor(
                mock(OsgiServiceUtils.class),
                mock(DataSourceTypeResolver.class),
                mock(Digester.class)
        )
        {
            @Override
            public String getPluginKey()
            {
                return "a-plugin-key";
            }
        };

    }

    @After
    public void tearDown()
    {
        moduleDescriptor = null;
    }

    @Test
    public void testValidateEntitiesThrowExceptionIfHGeneratedTableNameIdLongerThan30Chars()
    {
        final String tableName = "some-long-string-just-over-30-c";
        final TableNameConverter tableNameConverter = mock(TableNameConverter.class);
        when(tableNameConverter.getName(SomeEntity.class)).thenReturn(tableName);

        try
        {
            moduleDescriptor.validateEntities(getEntities(), tableNameConverter);
            fail("This should have thrown an exception, if indeed " + tableName.length() + " is greater than 30");
        }
        catch (PluginException e)
        {
            // expected
        }
    }

    private Set<Class<? extends RawEntity<?>>> getEntities()
    {
        return Sets.<Class<? extends RawEntity<?>>>newHashSet(SomeEntity.class);
    }

    private static interface SomeEntity extends Entity
    {
    }
}
