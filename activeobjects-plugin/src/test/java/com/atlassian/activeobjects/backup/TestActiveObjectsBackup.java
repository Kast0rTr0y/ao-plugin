package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.ao.ActiveObjectsFieldNameConverter;
import com.atlassian.activeobjects.test.model.Model;
import net.java.ao.EntityManager;
import net.java.ao.test.NameConverters;
import net.java.ao.test.jdbc.DynamicJdbcConfiguration;
import net.java.ao.test.jdbc.Jdbc;
import net.java.ao.test.jdbc.NonTransactional;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

@RunWith(ActiveObjectsJUnitRunner.class)
@Jdbc(DynamicJdbcConfiguration.class)
@NameConverters(table = BackupActiveObjectsTableNameConverter.class, field = ActiveObjectsFieldNameConverter.class)
public class TestActiveObjectsBackup
{
    private static final String HSQL = "/com/atlassian/activeobjects/backup/hsql.xml";
    private static final String MYSQL = "/com/atlassian/activeobjects/backup/mysql.xml";
    private static final String ORACLE = "/com/atlassian/activeobjects/backup/oracle.xml";
    private static final String POSTGRES = "/com/atlassian/activeobjects/backup/postgres.xml";

    private static final String UTF_8 = "UTF-8";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private EntityManager entityManager;
    private ActiveObjectsBackup aoBackup;
    private Model model;

    @Test
    @NonTransactional
    public void testHsqlBackup() throws Exception
    {
        testBackup(HSQL);
    }

    @Test
    @NonTransactional
    public void testMySqlBackup() throws Exception
    {
        testBackup(MYSQL);
    }

    @Test
    public void testPostgresBackup() throws Exception
    {
        testBackup(POSTGRES);
    }

    @Test
    public void testOracleBackup() throws Exception
    {
        testBackup(ORACLE);
    }

    public void testBackup(String xml) throws Exception
    {
        final String xmlBackup = read(xml);

        restore(xmlBackup);

        assertDataPresent();

        final String secondXmlBackup = save();
//        assertEquals(strip(xmlBackup), strip(secondXmlBackup));
//        TODO, once we're happy we're restoring data correctly, we should check that saving again, gives us the 'same' backup
    }

    private String save()
    {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        aoBackup.save(os);
        try
        {
            return os.toString(UTF_8);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void restore(String xmlBackup) throws IOException
    {
        aoBackup.restore(IOUtils.toInputStream(xmlBackup, UTF_8));
    }

    private String strip(String xmlBackup)
    {
        return xmlBackup.replaceAll("\n", "");
    }

    private void assertDataPresent()
    {
//        model.migrateEntities();
        model.checkAuthors();
        model.checkBooks();
    }

    @Before
    public void setUp()
    {
        aoBackup = new ActiveObjectsBackup(entityManager.getProvider());
        model = new Model(entityManager);
        model.emptyDatabase();
    }

    @After
    public void tearDown()
    {
        aoBackup = null;
        model = null;
    }

    private String read(String resource) throws IOException
    {
        logger.debug("Reading resource from '{}'", resource);
        InputStream is = null;
        try
        {
            is = this.getClass().getResourceAsStream(resource);
            return IOUtils.toString(is, UTF_8);
        }
        finally
        {
            IOUtils.closeQuietly(is);
        }
    }
}
