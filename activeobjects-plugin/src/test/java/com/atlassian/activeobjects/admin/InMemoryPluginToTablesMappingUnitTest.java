package com.atlassian.activeobjects.admin;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class InMemoryPluginToTablesMappingUnitTest
{
    private InMemoryPluginToTablesMapping pluginToTablesMapping;

    @Before
    public void setUp() throws Exception
    {
        pluginToTablesMapping = new InMemoryPluginToTablesMapping();
    }

    @Test
    public void addGet()
    {
        final PluginInfo pluginInfo = mock(PluginInfo.class);

        pluginToTablesMapping.add(pluginInfo, ImmutableList.of("tableName1", "tableName2"));

        assertThat(pluginToTablesMapping.pluginInfoByTableName, hasEntry("tableName1", pluginInfo));
        assertThat(pluginToTablesMapping.pluginInfoByTableName, hasEntry("tableName2", pluginInfo));

        assertThat(pluginToTablesMapping.get("tableName1"), is(pluginInfo));
        assertThat(pluginToTablesMapping.get("tableName2"), is(pluginInfo));
        assertThat(pluginToTablesMapping.get("tableName3"), nullValue());
    }
}
