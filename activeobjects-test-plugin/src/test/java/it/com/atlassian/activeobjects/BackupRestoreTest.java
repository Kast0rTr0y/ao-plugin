package it.com.atlassian.activeobjects;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import static com.atlassian.activeobjects.testplugin.BackupTestServlet.*;
import static org.junit.Assert.*;

public final class BackupRestoreTest
{
    private static final String BASE_URL = System.getProperty("baseurl");
    private static final String AO_TEST = BASE_URL + "/plugins/servlet/ao-test";

    private HttpClient client;
    public static final String EMPTY_BACKUP = "<?xml version='1.0' encoding='UTF-8'?><backup xmlns=\"http://www.atlassian.com/ao\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";

    @Test
    public void testBackup() throws Exception
    {
        final String backup = get(AO_TEST, parameters(CREATE, true)); // initial backup with creation of data
        assertBackupIsNotEmpty(backup);

        delete(AO_TEST);
        final String empty = get(AO_TEST, parameters(CREATE, false));
        assertBackupIsEmpty(empty.trim());

        post(AO_TEST, parameters(BACKUP, backup));  // restoring

        final String backupAfterRestore = get(AO_TEST, parameters(CREATE, false));
        assertEquals(backup, backupAfterRestore);
    }

    private void assertBackupIsEmpty(String backup)
    {
        assertEquals(EMPTY_BACKUP, backup);
    }

    private void assertBackupIsNotEmpty(String backup)
    {
        assertTrue(backup.trim().length() > EMPTY_BACKUP.length());
    }

    @Before
    public final void createHttpClient()
    {
        if (BASE_URL == null || BASE_URL.equals(""))
        {
            throw new IllegalStateException("BASE_URL is not set properly!");
        }
        client = new HttpClient();
    }

    private String get(String path, Map<String, Object> parameters) throws IOException
    {
        return service(newGetMethod(path, parameters));
    }

    private HttpMethod newGetMethod(String path, Map<String, Object> parameters)
    {
        final GetMethod method = new GetMethod(path);
        method.setQueryString(toNameValuePairArray(parameters));
        return method;
    }

    private NameValuePair[] toNameValuePairArray(Map<String, Object> parameters)
    {
        return Collections2.transform(parameters.entrySet(), new Function<Map.Entry<String, Object>, NameValuePair>()
        {
            public NameValuePair apply(Map.Entry<String, Object> from)
            {
                return new NameValuePair(from.getKey(), from.getValue().toString());
            }
        }).toArray(new NameValuePair[parameters.size()]);
    }

    private void post(String path, Map<String, Object> parameters) throws IOException
    {
        service(newPostMethod(path, parameters));
    }

    private HttpMethod newPostMethod(String path, Map<String, Object> parameters)
    {
        final PostMethod method = new PostMethod(path);
        method.setRequestBody(toNameValuePairArray(parameters));
        return method;
    }

    private void delete(String path) throws IOException
    {
        service(new DeleteMethod(path));
    }

    private String service(HttpMethod method) throws IOException
    {
        final int statusCode = client.executeMethod(method);
        if (statusCode != HttpStatus.SC_OK)
        {
            throw new RuntimeException("Got status " + statusCode + " for " + method.getName() + " on URI " + method.getURI());
        }
        return IOUtils.toString(method.getResponseBodyAsStream());
    }

    private Map<String, Object> parameters(String s, Object o)
    {
        return ImmutableMap.of(s, o);
    }

    /** A partial copy of IOUtils from commons-io */
    private static class IOUtils
    {
        private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

        public static String toString(InputStream input) throws IOException
        {
            StringWriter sw = new StringWriter();
            copy(input, sw);
            return sw.toString();
        }

        public static void copy(InputStream input, Writer output) throws IOException
        {
            InputStreamReader in = new InputStreamReader(input);
            copy(in, output);
        }

        public static int copy(Reader input, Writer output) throws IOException
        {
            long count = copyLarge(input, output);
            if (count > Integer.MAX_VALUE)
            {
                return -1;
            }
            return (int) count;
        }

        public static long copyLarge(Reader input, Writer output) throws IOException
        {
            char[] buffer = new char[DEFAULT_BUFFER_SIZE];
            long count = 0;
            int n = 0;
            while (-1 != (n = input.read(buffer)))
            {
                output.write(buffer, 0, n);
                count += n;
            }
            return count;
        }
    }
}
