package com.atlassian.dbexporter.node.stax;

import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.dbexporter.node.NodeParser;
import com.atlassian.dbexporter.node.NodeStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import static com.atlassian.dbexporter.node.stax.StaxUtils.*;
import static com.google.common.base.Preconditions.*;

/**
 * Reader implementation using StAX.
 *
 * @author Erik van Zijst
 */
public final class StaxStreamReader implements NodeStreamReader
{
    private static final String XMLSCHEMA_URI = "http://www.w3.org/2001/XMLSchema-instance";

    private final ImportExportErrorService errorService;
    private final XMLStreamReader reader;

    public StaxStreamReader(ImportExportErrorService errorService, Reader input)
    {
        this.errorService = checkNotNull(errorService);
        this.reader = createXmlStreamReader(checkNotNull(input));
    }

    private XMLStreamReader createXmlStreamReader(Reader reader)
    {
        try
        {
            return newXmlInputFactory().createXMLStreamReader(reader);
        }
        catch (XMLStreamException e)
        {
            throw errorService.newParseException(e);
        }
    }

    public NodeParser getRootNode()
    {
        if (reader.getEventType() != XMLStreamConstants.START_DOCUMENT)
        {
            throw new IllegalStateException("The root node has already been returned.");
        }
        else
        {
            try
            {
                reader.nextTag();

                return new NodeParser()
                {
                    public String getAttribute(String key)
                    {
                        return getAttribute(key, null, false);
                    }

                    public String getRequiredAttribute(String key)
                    {
                        return getAttribute(key, null, true);
                    }

                    private String getAttribute(String key, String namespaceUri, boolean required)
                    {
                        requireStartElement();
                        for (int i = 0; i < reader.getAttributeCount(); i++)
                        {
                            if (key.equals(reader.getAttributeName(i).getLocalPart()) &&
                                    (namespaceUri == null || namespaceUri.equals(reader.getAttributeName(i).getNamespaceURI())))
                            {
                                return unicodeDecode(reader.getAttributeValue(i));
                            }
                        }
                        if (required)
                        {
                            throw errorService.newParseException(String.format("Required attribute %s not found in node %s", key, getName()));
                        }
                        else
                        {
                            return null;
                        }
                    }

                    public String getName()
                    {
                        return reader.getLocalName();
                    }

                    public boolean isClosed()
                    {
                        return reader.getEventType() == XMLStreamConstants.END_ELEMENT || reader.getEventType() == XMLStreamConstants.END_DOCUMENT;
                    }

                    private int nextTagOrEndOfDocument()
                    {
                        try
                        {
                            int eventType = reader.next();
                            while ((eventType == XMLStreamConstants.CHARACTERS && reader.isWhiteSpace()) // skip whitespace
                                    || (eventType == XMLStreamConstants.CDATA && reader.isWhiteSpace())
                                    // skip whitespace
                                    || eventType == XMLStreamConstants.SPACE
                                    || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                                    || eventType == XMLStreamConstants.COMMENT)
                            {
                                eventType = reader.next();
                            }
                            if (eventType != XMLStreamConstants.START_ELEMENT &&
                                    eventType != XMLStreamConstants.END_ELEMENT &&
                                    eventType != XMLStreamConstants.END_DOCUMENT)
                            {
                                throw errorService.newParseException(
                                        "Unable to find start or end tag, or end of document. Location: " +
                                                reader.getLocation());
                            }
                            return eventType;
                        }
                        catch (XMLStreamException e)
                        {
                            throw errorService.newParseException(e);
                        }
                    }

                    public NodeParser getNextNode()
                    {
                        int event = nextTagOrEndOfDocument();

                        assert reader.isStartElement() || reader.isEndElement() ||
                                XMLStreamConstants.END_DOCUMENT == reader.getEventType();

                        return XMLStreamConstants.END_DOCUMENT == event ?
                                null : this;
                    }

                    public String getContentAsString()
                    {
                        requireStartElement();
                        try
                        {
                            if (Boolean.parseBoolean(getAttribute("nil", XMLSCHEMA_URI, false)))
                            {
                                nextTagOrEndOfDocument();
                                return null;
                            }
                            else
                            {
                                return unicodeDecode(reader.getElementText());
                            }
                        }
                        catch (XMLStreamException e)
                        {
                            throw errorService.newParseException(e);
                        }
                    }

                    public Boolean getContentAsBoolean()
                    {
                        String value = getContentAsString();
                        return value == null ? null : Boolean.parseBoolean(value);
                    }

                    public Date getContentAsDate()
                    {
                        String value = getContentAsString();
                        try
                        {
                            return value == null ? null : newDateFormat().parse(value);
                        }
                        catch (java.text.ParseException pe)
                        {
                            throw errorService.newParseException(pe);
                        }
                    }

                    public BigInteger getContentAsBigInteger()
                    {
                        String value = getContentAsString();
                        return value == null ? null : new BigInteger(value);
                    }

                    @Override
                    public BigDecimal getContentAsBigDecimal()
                    {
                        String value = getContentAsString();
                        return value == null ? null : new BigDecimal(value);
                    }

                    public void getContent(Writer writer)
                    {
                        throw new AssertionError("Not implemented.");
                    }

                    private void requireStartElement() throws IllegalStateException
                    {
                        if (!reader.isStartElement())
                        {
                            throw new IllegalStateException("Not currently positioned " +
                                    "at the start of a node.");
                        }
                    }

                    @Override
                    public String toString()
                    {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("<");
                        if (isClosed())
                        {
                            sb.append("/");
                        }
                        sb.append(getName());
                        if (!isClosed())
                        {
                            for (int i = 0; i < reader.getAttributeCount(); i++)
                            {
                                sb.append(" ")
                                        .append(reader.getAttributeName(i).getLocalPart())
                                        .append("=\"")
                                        .append(unicodeDecode(reader.getAttributeValue(i)))
                                        .append("\"");
                            }
                        }
                        sb.append(">");
                        return sb.toString();
                    }
                };
            }
            catch (XMLStreamException e)
            {
                throw errorService.newParseException(e);
            }
        }
    }

    public void close()
    {
        try
        {
            reader.close();
        }
        catch (XMLStreamException e)
        {
            throw errorService.newParseException(e);
        }
    }
}
