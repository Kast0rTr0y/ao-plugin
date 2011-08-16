package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.internal.Prefix;
import com.google.common.collect.Iterables;
import net.java.ao.DatabaseProvider;
import net.java.ao.SchemaConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.google.common.collect.Lists.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Testing {@link ActiveObjectsHashesReader}
 */
@RunWith(MockitoJUnitRunner.class)
public final class ActiveObjectsHashesReaderTest
{
    private ActiveObjectsHashesReader hashesReader;

    @Mock
    private ActiveObjectsTableNamesReader tableNamesReader;

    @Before
    public void setUp() throws Exception
    {
        hashesReader = new ActiveObjectsHashesReader(tableNamesReader);
    }

    @Test
    public void getHashesWithNoTables()
    {
        whenGetTableNamesThenReturn();

        assertTrue(Iterables.isEmpty(getHashes()));
    }

    @Test
    public void getHashesWithAllTablesWithSamePrefix()
    {
        whenGetTableNamesThenReturn("AO_HASH1_TABLE1", "AO_HASH1_TABLE2");

        final Iterable<String> hashes = getHashes();
        assertEquals(1, Iterables.size(hashes));
        assertEquals("HASH1", Iterables.get(hashes, 0));
    }

    @Test
    public void getHashesWithNonAoTables()
    {
        whenGetTableNamesThenReturn("AO_HASH1_TABLE1", "NONAO_HASH1_TABLE");

        final Iterable<String> hashes = getHashes();
        assertEquals(1, Iterables.size(hashes));
        assertEquals("HASH1", Iterables.get(hashes, 0));
    }

    @Test
    public void getHashesWithMultipleHashes()
    {
        whenGetTableNamesThenReturn("AO_HASH1_TABLE1", "AO_HASH2_TABLE2");

        final Iterable<String> hashes = getHashes();
        assertEquals(2, Iterables.size(hashes));
        assertEquals("HASH1", Iterables.get(hashes, 0));
        assertEquals("HASH2", Iterables.get(hashes, 1));
    }

    private Iterable<String> getHashes()
    {
        final PrefixedSchemaConfigurationFactory pscf = mock(PrefixedSchemaConfigurationFactory.class);
        final SchemaConfiguration sc = mock(SchemaConfiguration.class);
        final DatabaseProvider dp = mock(DatabaseProvider.class);

        when(pscf.getSchemaConfiguration(Matchers.<Prefix>any())).thenReturn(sc);
        return hashesReader.getHashes(dp, pscf);
    }

    private void whenGetTableNamesThenReturn(String... value)
    {
        when(tableNamesReader.getTableNames(Matchers.<DatabaseProvider>any(), Matchers.<SchemaConfiguration>any())).thenReturn(newArrayList(value));
    }
}
