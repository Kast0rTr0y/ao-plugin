package com.atlassian.dbexporter.node.stax;

import com.atlassian.dbexporter.ImportExportErrorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.bind.DatatypeConverter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

@RunWith(MockitoJUnitRunner.class)
public class StaxStreamWriterTest
{
    @Mock private ImportExportErrorService errorService;
    private Writer output;

    private StaxStreamWriter staxStreamWriter;

    @Before
    public void setUp() throws Exception
    {
       output = new StringWriter();
       staxStreamWriter = new StaxStreamWriter(errorService, output, Charset.forName("utf-8"), "");

    }

    @Test
    public void nodeCreatorShouldEncodeBinaryAsBase64() throws Exception
    {
        final byte[] bytes = new byte[] {0, 1, 100, 2};

        staxStreamWriter.addRootNode("root").setContentAsBinary(bytes);
        staxStreamWriter.close();

        assertThat(output.toString(), containsString(DatatypeConverter.printBase64Binary(bytes)));
    }
}
