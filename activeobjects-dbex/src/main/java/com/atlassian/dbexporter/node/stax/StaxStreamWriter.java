package com.atlassian.dbexporter.node.stax;

import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.node.NodeCreator;
import com.atlassian.dbexporter.node.NodeStreamWriter;
import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Date;

import static com.atlassian.dbexporter.node.stax.StaxUtils.*;
import static com.google.common.base.Preconditions.*;

/**
 * Writer implementation using StAX.
 *
 * @author Erik van Zijst
 */
public final class StaxStreamWriter implements NodeStreamWriter
{
    private static final String XMLSCHEMA_URI = "http://www.w3.org/2001/XMLSchema-instance";

    private final ImportExportErrorService errorService;
    private final XMLStreamWriter writer;
    private final String nameSpaceUri;
    private final Charset charset;
    private boolean rootExists = false;

    /**
     * Creates a new StAX document with the default namespace set to the specified
     * uri.
     */
    public StaxStreamWriter(ImportExportErrorService errorService, Writer output, Charset charset, String nameSpaceUri)
    {
        this.errorService = checkNotNull(errorService);
        this.writer = new IndentingXMLStreamWriter(checkNotNull(createXmlStreamWriter(output)));
        this.charset = checkNotNull(charset);
        this.nameSpaceUri = checkNotNull(nameSpaceUri);
    }

    private XMLStreamWriter createXmlStreamWriter(Writer writer)
    {
        try
        {
            return newXmlOutputFactory().createXMLStreamWriter(writer);
        }
        catch (XMLStreamException xe)
        {
            throw errorService.newParseException(xe);
        }
    }

    public NodeCreator addRootNode(String name)
    {
        if (rootExists)
        {
            throw new IllegalStateException("Root node already created.");
        }
        else
        {
            try
            {
                writer.writeStartDocument(charset.name(), "1.0");
                rootExists = true;

                NodeCreator nc = new NodeCreator()
                {
                    private long depth = 0L;

                    public NodeCreator addNode(String name)
                    {
                        try
                        {
                            writer.writeStartElement(name);
                            depth++;
                            return this;
                        }
                        catch (XMLStreamException e)
                        {
                            throw errorService.newParseException(e);
                        }
                    }

                    public NodeCreator closeEntity()
                    {
                        try
                        {
                            writer.writeEndElement();
                            return --depth == 0L ? null : this;
                        }
                        catch (XMLStreamException e)
                        {
                            throw errorService.newParseException(e);
                        }
                    }

                    public NodeCreator setContentAsDate(Date date)
                    {
                        return setContentAsString(date == null ? null : newDateFormat().format(date));
                    }

                    @Override
                    public NodeCreator setContentAsBigInteger(BigInteger bigInteger)
                    {
                        return setContentAsString(bigInteger == null ? null : bigInteger.toString());
                    }

                    @Override
                    public NodeCreator setContentAsBigDecimal(BigDecimal bigDecimal)
                    {
                        return setContentAsString(bigDecimal == null ? null : bigDecimal.toString());
                    }

                    @Override
                    public NodeCreator setContentAsBoolean(Boolean bool)
                    {
                        return setContentAsString(bool == null ? null : Boolean.toString(bool));
                    }

                    public NodeCreator setContentAsString(String value)
                    {
                        try
                        {
                            if (value == null)
                            {
                                writer.writeAttribute(XMLSCHEMA_URI, "nil", "true");
                            }
                            else
                            {
                                writer.writeCharacters(unicodeEncode(value));
                            }
                            return this;
                        }
                        catch (XMLStreamException e)
                        {
                            throw errorService.newParseException(e);
                        }
                    }

                    public NodeCreator setContent(Reader data)
                    {
                        throw new AssertionError("Not implemented");
                    }

                    public NodeCreator addAttribute(String key, String value)
                    {
                        try
                        {
                            writer.writeAttribute(key, unicodeEncode(value));
                            return this;
                        }
                        catch (XMLStreamException e)
                        {
                            throw errorService.newParseException(e);
                        }
                    }
                };
                NodeCreator nodeCreator = nc.addNode(name);
                writer.writeDefaultNamespace(nameSpaceUri);
                writer.writeNamespace("xsi", XMLSCHEMA_URI);
                return nodeCreator;
            }
            catch (XMLStreamException e)
            {
                throw errorService.newParseException("Unable to create the root node.", e);
            }
        }
    }

    public void flush()
    {
        try
        {
            writer.flush();
        }
        catch (XMLStreamException e)
        {
            throw errorService.newParseException(e);
        }
    }

    public void close()
    {
        try
        {
            writer.close();
        }
        catch (XMLStreamException e)
        {
            throw errorService.newParseException(e);
        }
    }
}
