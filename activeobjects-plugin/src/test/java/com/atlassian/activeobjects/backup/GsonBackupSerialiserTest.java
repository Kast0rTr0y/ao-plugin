package com.atlassian.activeobjects.backup;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import net.java.ao.schema.ddl.DDLAction;
import net.java.ao.schema.ddl.DDLActionType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Testing {@link com.atlassian.activeobjects.backup.GsonBackupSerialiser}
 */
public class GsonBackupSerialiserTest
{
    private static final String UTF_8 = "UTF-8";

    private BackupSerialiser<Collection<DDLAction>> serialiser;

    @Before
    public void setUp() throws Exception
    {
        serialiser = new GsonBackupSerialiser<Collection<DDLAction>>(new TypeToken<Collection<DDLAction>>()
        {
        }.getType());
    }

    @After
    public void tearDown() throws Exception
    {
        serialiser = null;
    }

    @Test
    public void testSerialiseNull() throws Exception
    {
        assertEquals("", serialise(null));
    }

    @Test
    public void testDeserialiseEmptyStream() throws Exception
    {
        assertNull(deserialise(""));
    }

    @Test
    public void testSerialiseSingleDdlAction() throws Exception
    {
        DdlActionsAndJson aaj = getSingleAction();
        assertEquals(aaj.json, serialise(aaj.actions));
    }

    @Test
    public void testDeserialiseSingleDdlAction() throws Exception
    {
        final DdlActionsAndJson aaj = getSingleAction();
        final Iterator<DDLAction> iterator = deserialise(aaj.json).iterator();

        assertTrue(iterator.hasNext());
        assertEquals(aaj.actions.iterator().next().getActionType(), iterator.next().getActionType());
    }

    private DdlActionsAndJson getSingleAction()
    {
        return new DdlActionsAndJson(iterable(new DDLAction(DDLActionType.CREATE)), "[{\"actionType\":\"CREATE\"}]");
    }

    private static class DdlActionsAndJson
    {

        public final Collection<DDLAction> actions;
        public final String json;

        public DdlActionsAndJson(Collection<DDLAction> actions, String json)
        {
            this.actions = actions;
            this.json = json;
        }
    }

    private static Collection<DDLAction> iterable(DDLAction... actions)
    {
        return Lists.newArrayList(actions);
    }

    private String serialise(Collection<DDLAction> o)
    {
        ByteArrayOutputStream os = null;
        try
        {
            os = new ByteArrayOutputStream();
            serialiser.serialise(o, os);
            return toString(os.toByteArray());
        }
        finally
        {
            closeQuietly(os);
        }
    }

    private Iterable<DDLAction> deserialise(String s)
    {
        InputStream is = null;
        try
        {
            is = toInsputStream(s == null ? "" : s);
            return serialiser.deserialise(is);
        }
        finally
        {
            closeQuietly(is);
        }
    }

    private static InputStream toInsputStream(String s)
    {
        try
        {
            return new ByteArrayInputStream(s.trim().getBytes(UTF_8));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }


    private static String toString(byte[] bytes)
    {
        try
        {
            return new String(bytes, UTF_8).trim();
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void closeQuietly(Closeable c)
    {
        if (c != null)
        {
            try
            {
                c.close();
            }
            catch (IOException e)
            {
                // ignored
            }
        }
    }
}
