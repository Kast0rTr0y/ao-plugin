package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.NoOpEntityNameProcessor;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import com.atlassian.dbexporter.node.NodeParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TableDefinitionImporterTest
{
    @Rule
    public NodeParserRule nodeParser = new NodeParserRule();

    private TableDefinitionImporter tableDefinitionImporter;

    private Context context;

    @Mock
    private ImportExportErrorService errorService;

    @Mock
    private TableCreator tableCreator;

    @Mock
    private DatabaseCleaner databaseCleaner;

    @Mock
    private ProgressMonitor monitor;

    @Mock
    private ImportConfiguration configuration;

    @Test
    @Xml(SINGLE_TABLE)
    public void singleTableDefinition() throws Exception
    {
        final NodeParser node = nodeParser.getNode();
        tableDefinitionImporter.doImportNode(node, configuration, context);

        final List<Table> tables = verifyTables();
        assertEquals(1, tables.size());

        assertTable(tables.iterator().next(), "a-table", 2, "column-1", "column-2");
    }

    @Test
    @Xml(MULTIPLE_TABLES)
    public void multipleTableDefinitions() throws Exception
    {
        final NodeParser node = nodeParser.getNode();
        tableDefinitionImporter.doImportNode(node.getNextNode(), configuration, context);

        assertTrue(node.isClosed());

        final List<Table> tables = verifyTables();
        assertEquals(3, tables.size());

        final Iterator<Table> iterator = tables.iterator();
        assertTable(iterator.next(), "table-1", 1, "column-11");
        assertTable(iterator.next(), "table-2", 2, "column-21", "column-22");
        assertTable(iterator.next(), "table-3", 3, "column-31", "column-32", "column-33");
    }

    @SuppressWarnings("unchecked")
    private List<Table> verifyTables()
    {
        final ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(tableCreator).create(argument.capture(), Matchers.<EntityNameProcessor>any(), Matchers.<ProgressMonitor>any());
        return argument.getValue();
    }

    @Before
    public void setUp()
    {
        when(configuration.getProgressMonitor()).thenReturn(monitor);
        when(configuration.getEntityNameProcessor()).thenReturn(new NoOpEntityNameProcessor());
        context = new Context();
        tableDefinitionImporter = new TableDefinitionImporter(errorService, tableCreator, databaseCleaner);
    }

    @After
    public void tearDown()
    {
        tableDefinitionImporter = null;
        context = null;
    }

    private static final String SINGLE_TABLE =
            "<table name=\"a-table\">\n" +
                    "  <column name=\"column-1\" sqlType=\"3\"/>\n" +
                    "  <column name=\"column-2\" sqlType=\"5\"/>\n" +
                    "  <foreignKey name=\"fk-1\" fromTable=\"a-table\" toTable=\"table-2\" fromColumn=\"column-11\" toColumn=\"column-21\"/>\n" +
                    "</table>";


    private static final String MULTIPLE_TABLES = "<database>\n" +
            "<table name=\"table-1\">\n" +
            "  <column name=\"column-11\" sqlType=\"3\"/>\n" +
            "  <foreignKey name=\"fk-11\" fromTable=\"table-1\" toTable=\"table-2\" fromColumn=\"column-11\" toColumn=\"column-21\"/>\n" +
            "</table>\n" +
            "<table name=\"table-2\">\n" +
            "  <column name=\"column-21\" sqlType=\"3\"/>\n" +
            "  <column name=\"column-22\" sqlType=\"5\"/>\n" +
            "</table>\n" +
            "<table name=\"table-3\">\n" +
            "  <column name=\"column-31\" sqlType=\"3\"/>\n" +
            "  <column name=\"column-32\" sqlType=\"5\"/>\n" +
            "  <column name=\"column-33\" sqlType=\"7\"/>\n" +
            "</table>\n" +
            "</database>";

    private static void assertTable(Table table, String tableName, int columnsCount, String... columnNames)
    {
        assertEquals(tableName, table.getName());
        assertEquals(columnsCount, table.getColumns().size());
        final Iterator<Column> colIt = table.getColumns().iterator();
        for (String columnName : columnNames)
        {
            assertEquals(columnName, colIt.next().getName());
        }
    }
}
