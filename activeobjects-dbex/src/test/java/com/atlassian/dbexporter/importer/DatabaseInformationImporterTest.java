package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.DatabaseInformation;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseInformationImporterTest
{
    @Rule
    public NodeParserRule nodeParser = new NodeParserRule();

    private DatabaseInformationImporter importer;

    @Mock
    private DatabaseInformationChecker checker;

    @Mock
    private ImportConfiguration configuration;

    @Mock
    private ProgressMonitor progressMonitor;

    @Mock
    private ImportExportErrorService errorService;

    @Test
    @Xml("<database />")
    public void importEmptyInformation()
    {
        final Context context = new Context();
        importer.doImportNode(nodeParser.getNode(), configuration, context);

        final DatabaseInformation info = getDatabaseInformation(context);
        assertNotNull(info);
        assertTrue(info.isEmpty());
    }

    @Test
    @Xml("<database><meta key=\"a-key\" value=\"some-value\"/></database>")
    public void importMetaInformation()
    {
        final Context context = new Context();
        importer.doImportNode(nodeParser.getNode(), configuration, context);

        final DatabaseInformation info = getDatabaseInformation(context);
        assertNotNull(info);
        assertFalse(info.isEmpty());
        assertEquals("some-value", info.getString("a-key"));
    }

    @Before
    public void setUp() throws Exception
    {
        when(configuration.getProgressMonitor()).thenReturn(progressMonitor);

        importer = new DatabaseInformationImporter(errorService, checker);
    }

    @After
    public void tearDown() throws Exception
    {
        importer = null;
    }

    private DatabaseInformation getDatabaseInformation(Context context)
    {
        return context.get(DatabaseInformation.class);
    }
}
