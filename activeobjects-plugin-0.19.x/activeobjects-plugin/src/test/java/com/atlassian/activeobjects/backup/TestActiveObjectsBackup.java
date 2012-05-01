package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.test.model.Model;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.java.ao.DatabaseProvider;
import net.java.ao.db.HSQLDatabaseProvider;
import net.java.ao.db.MySQLDatabaseProvider;
import net.java.ao.db.OracleDatabaseProvider;
import net.java.ao.db.PostgreSQLDatabaseProvider;
import net.java.ao.db.SQLServerDatabaseProvider;
import net.java.ao.test.jdbc.NonTransactional;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.sql.Types;

import static org.custommonkey.xmlunit.XMLAssert.*;

public final class TestActiveObjectsBackup extends AbstractTestActiveObjectsBackup
{
    private static final String HSQL = "/com/atlassian/activeobjects/backup/hsql.xml";
    private static final String MYSQL = "/com/atlassian/activeobjects/backup/mysql.xml";
    private static final String ORACLE = "/com/atlassian/activeobjects/backup/oracle.xml";
    private static final String LEGACY_ORACLE = "/com/atlassian/activeobjects/backup/legacy_oracle.xml";
    private static final String POSTGRES = "/com/atlassian/activeobjects/backup/postgres.xml";
    private static final String SQLSERVER = "/com/atlassian/activeobjects/backup/sqlserver.xml";

    private Model model;

    @Test
    @NonTransactional
    public void testHsqlBackup() throws Exception
    {
        testBackup(HSQL, HSQL_DATA);
    }

    @Test
    @NonTransactional
    public void testMySqlBackup() throws Exception
    {
        testBackup(MYSQL, MYSQL_DATA);
    }

    @Test
    @NonTransactional
    public void testPostgresBackup() throws Exception
    {
        testBackup(POSTGRES, POSTGRES_DATA);
    }

    @Test
    @NonTransactional
    public void testOracleBackup() throws Exception
    {
        testBackup(ORACLE, ORACLE_DATA);
    }

    @Test
    @NonTransactional
    public void testLegacyOracleBackup() throws Exception
    {
        testBackup(LEGACY_ORACLE, LEGACY_ORACLE_DATA);
    }

    @Test
    @NonTransactional
    public void testSqlServerBackup() throws Exception
    {
        testBackup(SQLSERVER, SQL_SERVER_DATA);
    }

    public final void testBackup(String xml, Iterable<BackupData> data) throws Exception
    {
        final String xmlBackup = read(xml);

        checkXmlBackup(xmlBackup, data);

        restore(xmlBackup);
        restore(xmlBackup); // if we don't clean up correctly doing an second restore in a row will fail

        assertDataPresent();

        final String secondXmlBackup = save();

        checkXmlBackup(secondXmlBackup, getCurrentDatabaseData());
    }

    private Iterable<BackupData> getCurrentDatabaseData()
    {
        final DatabaseProvider provider = entityManager.getProvider();
        if (provider instanceof HSQLDatabaseProvider)
        {
            return HSQL_DATA;
        }
        else if (provider instanceof MySQLDatabaseProvider)
        {
            return MYSQL_DATA;
        }
        else if (provider instanceof PostgreSQLDatabaseProvider)
        {
            return POSTGRES_DATA;
        }
        else if (provider instanceof OracleDatabaseProvider)
        {
            return ORACLE_DATA;
        }
        else if (provider instanceof SQLServerDatabaseProvider)
        {
            return SQL_SERVER_DATA;
        }
        else
        {
            throw new IllegalStateException("Can't figure out which DB we're testing against!");
        }
    }

    private void checkXmlBackup(String xmlBackup, Iterable<BackupData> data) throws Exception
    {
        final XpathEngine engine = newXpathEngine();
        final Document doc = XMLUnit.buildControlDocument(xmlBackup);

        for (BackupData bd : data)
        {
            assertHasBackupData(engine, doc, bd);
        }
    }

    private void assertHasBackupData(XpathEngine xpathEngine, Document xmlBackupDoc, BackupData bd) throws Exception
    {
        assertHasColumn(xpathEngine, xmlBackupDoc, bd.table, bd.column, bd.primaryKey, bd.autoIncrement, bd.sqlType);
    }

    private void assertHasColumn(XpathEngine xpathEngine, Document xmlBackupDoc, String tableName, String columnName, boolean pK, boolean autoIncrement, SqlType sqlType) throws XpathException
    {
        NodeList tableNodes = xpathEngine.getMatchingNodes("/ao:backup/ao:table[@name='" + tableName + "']/ao:column[@name='" + columnName + "']", xmlBackupDoc);
        assertEquals(1, tableNodes.getLength());
        Node table = tableNodes.item(0);
        assertAttributeEquals("Expected " + tableName + "." + columnName + " to " + (pK ? "" : "NOT ") + "be a primary key.", table, "primaryKey", pK);
        assertAttributeEquals("Expected " + tableName + "." + columnName + " to " + (autoIncrement ? "" : "NOT ") + "be auto increment.", table, "autoIncrement", autoIncrement);
        assertAttributeEquals("Expected " + tableName + "." + columnName + " to be of SQL Type: " + sqlType.type, table, "sqlType", sqlType.type);
        if (sqlType.precision != null)
        {
            assertAttributeEquals("Expected " + tableName + "." + columnName + " to have precision: " + sqlType.precision, table, "precision", sqlType.precision);
        }
        if (sqlType.scale != null)
        {
            assertAttributeEquals("Expected " + tableName + "." + columnName + " to have scale: " + sqlType.scale, table, "scale", sqlType.scale);
        }

        assertEquals(1, xpathEngine.getMatchingNodes("/ao:backup/ao:data[@tableName='" + tableName + "']/ao:column[@name='" + columnName + "']", xmlBackupDoc).getLength());
    }

    private void assertAttributeEquals(String message, Node table, String attribute, Object expected)
    {
        assertEquals(message, String.valueOf(expected), attributeValue(table, attribute));
    }

    private XpathEngine newXpathEngine()
    {
        XpathEngine engine = XMLUnit.newXpathEngine();
        engine.setNamespaceContext(new SimpleNamespaceContext(ImmutableMap.of("ao", "http://www.atlassian.com/ao")));
        return engine;
    }

    private String attributeValue(Node node, String name)
    {
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }

    private void assertDataPresent()
    {
        model.checkAuthors();
        model.checkBooks();
    }

    @Before
    public void setUpModel()
    {
        model = new Model(entityManager);
        model.emptyDatabase();
    }

    @After
    public void tearDownModel()
    {
        model = null;
    }

    private static final class BackupData
    {
        public final String table, column;
        public final boolean primaryKey;
        public final boolean autoIncrement;
        public final SqlType sqlType;

        public BackupData(String table, String column, boolean primaryKey, boolean autoIncrement, SqlType sqlType)
        {
            this.table = table;
            this.column = column;
            this.sqlType = sqlType;
            this.primaryKey = primaryKey;
            this.autoIncrement = autoIncrement;
        }

        static BackupData of(String table, String column, SqlType sqlType)
        {
            return of(table, column, sqlType, false, false);
        }

        static BackupData of(String table, String column, SqlType sqlType, boolean primaryKey, boolean autoIncrement)
        {
            return new BackupData(table, column, primaryKey, autoIncrement, sqlType);
        }

        static BackupData of(BackupData data, SqlType sqlType)
        {
            return of(data.table, data.column, sqlType, data.primaryKey, data.autoIncrement);
        }
    }

    private static final class SqlType
    {
        public final int type;
        public final Integer precision;
        public final Integer scale;

        public SqlType(int type, Integer precision, Integer scale)
        {
            this.type = type;
            this.precision = precision;
            this.scale = scale;
        }

        static SqlType of(int type)
        {
            return of(type, null);
        }

        private static SqlType of(int type, Integer precision)
        {
            return of(type, precision, null);
        }

        private static SqlType of(int type, Integer precision, Integer scale)
        {
            return new SqlType(type, precision, scale);
        }
    }

    private static final BackupData AUTHORSHIP_AUTHOR_ID = BackupData.of("AO_000000_AUTHORSHIP", "AUTHOR_ID", SqlType.of(Types.INTEGER));
    private static final BackupData AUTHORSHIP_BOOK_ID = BackupData.of("AO_000000_AUTHORSHIP", "BOOK_ID", SqlType.of(Types.BIGINT));
    private static final BackupData AUTHORSHIP_ID = BackupData.of("AO_000000_AUTHORSHIP", "ID", SqlType.of(Types.INTEGER), true, true);

    private static final BackupData BOOK_ABSTRACT = BackupData.of("AO_000000_BOOK", "ABSTRACT", SqlType.of(Types.LONGVARCHAR));
    private static final BackupData BOOK_ISBN = BackupData.of("AO_000000_BOOK", "ISBN", SqlType.of(Types.BIGINT), true, false);
    private static final BackupData BOOK_READ = BackupData.of("AO_000000_BOOK", "IS_READ", SqlType.of(Types.BOOLEAN));
    private static final BackupData BOOK_PAGES = BackupData.of("AO_000000_BOOK", "NUMBER_OF_PAGES", SqlType.of(Types.INTEGER));
    private static final BackupData BOOK_PRICE = BackupData.of("AO_000000_BOOK", "PRICE", SqlType.of(Types.DOUBLE));
    private static final BackupData BOOK_PUBLISHED = BackupData.of("AO_000000_BOOK", "PUBLISHED", SqlType.of(Types.TIMESTAMP));
    private static final BackupData BOOK_TITLE = BackupData.of("AO_000000_BOOK", "TITLE", SqlType.of(Types.VARCHAR, 255));

    private static final BackupData AUTHOR_NAME = BackupData.of("AO_000000_LONG_NAME_TO_AUTHOR", "NAME", SqlType.of(Types.VARCHAR, 60));
    private static final BackupData AUTHOR_ID = BackupData.of("AO_000000_LONG_NAME_TO_AUTHOR", "ID", SqlType.of(Types.INTEGER), true, true);

    private static Iterable<BackupData> HSQL_DATA = ImmutableList.of(
            AUTHORSHIP_AUTHOR_ID,
            AUTHORSHIP_BOOK_ID,
            AUTHORSHIP_ID,

            BOOK_ABSTRACT,
            BOOK_ISBN,
            BOOK_READ,
            BOOK_PAGES,
            BOOK_PRICE,
            BOOK_PUBLISHED,
            BOOK_TITLE,

            AUTHOR_NAME,
            AUTHOR_ID
    );

    private static Iterable<BackupData> MYSQL_DATA = ImmutableList.of(
            AUTHORSHIP_AUTHOR_ID,
            AUTHORSHIP_BOOK_ID,
            AUTHORSHIP_ID,

            BOOK_ABSTRACT,
            BOOK_ISBN,
            BackupData.of(BOOK_READ, SqlType.of(Types.BIT)),
            BOOK_PAGES,
            BOOK_PRICE,
            BOOK_PUBLISHED,
            BOOK_TITLE,

            AUTHOR_NAME,
            AUTHOR_ID
    );

    private static Iterable<BackupData> POSTGRES_DATA = ImmutableList.of(
            AUTHORSHIP_AUTHOR_ID,
            AUTHORSHIP_BOOK_ID,
            AUTHORSHIP_ID,

            BackupData.of(BOOK_ABSTRACT, SqlType.of(Types.VARCHAR, -1)),
            BOOK_ISBN,
            BackupData.of(BOOK_READ, SqlType.of(Types.BIT)),
            BOOK_PAGES,
            BOOK_PRICE,
            BOOK_PUBLISHED,
            BOOK_TITLE,

            AUTHOR_NAME,
            AUTHOR_ID
    );

    private static Iterable<BackupData> ORACLE_DATA = ImmutableList.of(
            BackupData.of(AUTHORSHIP_AUTHOR_ID, SqlType.of(Types.NUMERIC, 11)),
            BackupData.of(AUTHORSHIP_BOOK_ID, SqlType.of(Types.NUMERIC, 20)),
            BackupData.of(AUTHORSHIP_ID, SqlType.of(Types.NUMERIC, 11)),

            BackupData.of(BOOK_ABSTRACT, SqlType.of(Types.CLOB)),
            BackupData.of(BOOK_ISBN, SqlType.of(Types.NUMERIC, 20)),
            BackupData.of(BOOK_READ, SqlType.of(Types.NUMERIC, 1)),
            BackupData.of(BOOK_PAGES, SqlType.of(Types.NUMERIC, 11)),
            BackupData.of(BOOK_PRICE, SqlType.of(Types.NUMERIC, 126)),
            BOOK_PUBLISHED,
            BOOK_TITLE,

            AUTHOR_NAME,
            BackupData.of(AUTHOR_ID, SqlType.of(Types.NUMERIC, 11))
    );

    private static Iterable<BackupData> LEGACY_ORACLE_DATA = ImmutableList.of(
            BackupData.of(AUTHORSHIP_AUTHOR_ID, SqlType.of(Types.NUMERIC, 11)),
            BackupData.of(AUTHORSHIP_BOOK_ID, SqlType.of(Types.NUMERIC, 20)),
            BackupData.of(AUTHORSHIP_ID, SqlType.of(Types.NUMERIC, 11)),

            BackupData.of(BOOK_ABSTRACT, SqlType.of(Types.CLOB)),
            BackupData.of(BOOK_ISBN, SqlType.of(Types.NUMERIC, 20)),
            BackupData.of(BOOK_READ, SqlType.of(Types.NUMERIC, 1)),
            BackupData.of(BOOK_PAGES, SqlType.of(Types.NUMERIC, 11)),
            BackupData.of(BOOK_PRICE, SqlType.of(Types.NUMERIC, 32, 16)),
            BOOK_PUBLISHED,
            BOOK_TITLE,

            AUTHOR_NAME,
            BackupData.of(AUTHOR_ID, SqlType.of(Types.NUMERIC, 11))
    );

    private static Iterable<BackupData> SQL_SERVER_DATA = ImmutableList.of(
            AUTHORSHIP_AUTHOR_ID,
            AUTHORSHIP_BOOK_ID,
            AUTHORSHIP_ID,

            BackupData.of(BOOK_ABSTRACT, SqlType.of(Types.CLOB)),
            BOOK_ISBN,
            BackupData.of(BOOK_READ, SqlType.of(Types.BIT)),
            BOOK_PAGES,
            BOOK_PRICE,
            BOOK_PUBLISHED,
            BOOK_TITLE,

            AUTHOR_NAME,
            AUTHOR_ID
    );
}
