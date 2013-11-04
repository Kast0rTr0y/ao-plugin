package com.atlassian.dbexporter.exporter;

import com.atlassian.dbexporter.Column;
import com.atlassian.dbexporter.ConnectionProvider;
import com.atlassian.dbexporter.Context;
import com.atlassian.dbexporter.EntityNameProcessor;
import com.atlassian.dbexporter.ForeignKey;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.Table;
import com.atlassian.dbexporter.node.NodeCreator;
import com.atlassian.dbexporter.progress.ProgressMonitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DataExporterTest
{
    @Mock private ImportExportErrorService errorService;
    @Mock private ExportConfiguration configuration;
    @Mock private ConnectionProvider connectionProvider;
    @Mock private Connection connection;
    @Mock private DatabaseMetaData metaData;
    @Mock private ProgressMonitor progressMonitor;
    @Mock private EntityNameProcessor entityNameProcessor;
    @Mock private NodeCreator nodeCreator;

    private DataExporter dataExporter;

    @Before
    public void setUp() throws Exception
    {
        when(configuration.getConnectionProvider()).thenReturn(connectionProvider);
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getIdentifierQuoteString()).thenReturn(" ");
        when(configuration.getProgressMonitor()).thenReturn(progressMonitor);
        when(configuration.getEntityNameProcessor()).thenReturn(entityNameProcessor);

        dataExporter = new DataExporter(errorService, null);
    }

    @Test
    public void shouldExportBinaryColumn() throws Exception
    {
        final byte[] bytes = new byte[] {0, 1, 100, 2};

        when(nodeCreator.addNode(anyString())).thenReturn(nodeCreator);
        when(nodeCreator.addAttribute(anyString(), anyString())).thenReturn(nodeCreator);
        when(nodeCreator.setContentAsBinary(Matchers.<byte[]>any())).thenReturn(nodeCreator);
        when(nodeCreator.closeEntity()).thenReturn(nodeCreator);
        Statement st = mock(Statement.class);
        when(connection.createStatement()).thenReturn(st);
        ResultSet rs = mock(ResultSet.class);
        when(st.executeQuery(anyString())).thenReturn(rs);
        ResultSetMetaData rsmd = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(rsmd);
        when(rsmd.getColumnCount()).thenReturn(1);
        when(rs.next()).thenReturn(true).thenReturn(false);
        when(rsmd.getColumnType(1)).thenReturn(Types.BINARY);
        when(rs.getBytes(1)).thenReturn(bytes);

        Context context = new Context(new Table("table",
                Collections.singletonList(new Column("column", Types.BINARY, null, null, null, null)),
                Collections.<ForeignKey>emptyList()));

        dataExporter.export(nodeCreator, configuration, context);

        verify(nodeCreator).setContentAsBinary(bytes);
    }
}
