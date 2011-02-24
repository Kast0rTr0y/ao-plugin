package com.atlassian.dbexporter.node.stax;

import com.atlassian.dbexporter.node.NodeCreator;
import com.atlassian.dbexporter.node.NodeStreamWriter;
import com.atlassian.dbexporter.node.ParseException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Reader;
import java.io.Writer;
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
    private final XMLStreamWriter writer;
    private final String nameSpaceUri;
    private final Charset charset;
    private boolean rootExists = false;

    /**
     * Creates a new StAX document with the default namespace set to the specified
     * uri.
     */
    public StaxStreamWriter(Writer output, Charset charset, String namespaceUri) throws ParseException
    {
        this(createXmlStreamWriter(output), charset, namespaceUri);
    }

    public StaxStreamWriter(XMLStreamWriter writer, Charset charset, String nameSpaceUri)
    {
        this.writer = checkNotNull(writer);
        this.charset = checkNotNull(charset);
        this.nameSpaceUri = checkNotNull(nameSpaceUri);
    }

    private static XMLStreamWriter createXmlStreamWriter(Writer writer) throws ParseException
    {
        try
        {
            return newXmlOutputFactory().createXMLStreamWriter(writer);
        }
        catch (XMLStreamException xe)
        {
            throw new ParseException(xe);
        }
    }

    public NodeCreator addRootNode(String name) throws ParseException, IllegalStateException
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

                    public NodeCreator addNode(String name) throws ParseException
                    {
                        try
                        {
                            writer.writeStartElement(name);
                            depth++;
                            return this;
                        }
                        catch (XMLStreamException e)
                        {
                            throw new ParseException(e);
                        }
                    }

                    public NodeCreator closeEntity() throws ParseException
                    {
                        try
                        {
                            writer.writeEndElement();
                            return --depth == 0L ? null : this;
                        }
                        catch (XMLStreamException e)
                        {
                            throw new ParseException(e);
                        }
                    }

                    public NodeCreator setContentAsDate(Date date) throws
                            ParseException
                    {
                        return setContentAsString(date == null ? null : newDateFormat().format(date));
                    }

                    public NodeCreator setContentAsBigInteger(BigInteger bigInteger)
                            throws ParseException
                    {
                        return setContentAsString(bigInteger == null ? null : bigInteger.toString());
                    }

                    public NodeCreator setContentAsBoolean(Boolean bool)
                            throws ParseException
                    {
                        return setContentAsString(bool == null ? null : Boolean.toString(bool));
                    }

                    public NodeCreator setContentAsString(String value) throws ParseException
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
                            throw new ParseException(e);
                        }
                    }

                    public NodeCreator setContent(Reader data)
                    {
                        throw new AssertionError("Not implemented");
                    }

                    public NodeCreator addAttribute(String key, String value) throws ParseException
                    {
                        try
                        {
                            writer.writeAttribute(key, unicodeEncode(value));
                            return this;
                        }
                        catch (XMLStreamException e)
                        {
                            throw new ParseException(e);
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
                throw new ParseException("Unable to create the root node.", e);
            }
        }
    }

    public void flush() throws ParseException
    {
        try
        {
            writer.flush();
        }
        catch (XMLStreamException e)
        {
            throw new ParseException(e);
        }
    }

    public void close() throws ParseException
    {
        try
        {
            writer.close();
        }
        catch (XMLStreamException e)
        {
            throw new ParseException(e);
        }
    }
}
