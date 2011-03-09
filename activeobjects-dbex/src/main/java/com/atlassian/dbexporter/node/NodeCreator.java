package com.atlassian.dbexporter.node;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * Represents a node in streaming XML documents. This is a small abstraction
 * layer over StAX that allows other streaming data formats to be created (JSON,
 * ASN.1, ?) and make it easier to deal with StAX, of which the API can be a bit
 * tedious. This interface also provides type conversion instead of just strings.
 * This interface only provides write access to a streaming node graph.
 *
 * @see NodeParser  counterpart of this interface that provides read access to
 * streaming node graphs.
 * @see NodeStreamWriter
 * @author Erik van Zijst
 */
public interface NodeCreator {

    /**
     * Creates a new child node under the current node.
     *
     * @param name  the name of the new child node.
     * @return  a reference to the new node. Continue with this reference.
     * @throws ParseException
     */
    NodeCreator addNode(String name) throws ParseException;

    /**
     * Closes the current node and returns a reference to the parent node.
     *
     * @return  a reference to the parent node. Continue with this reference.
     * @throws ParseException
     */
    NodeCreator closeEntity() throws ParseException;

    /**
     * Similar to {@link NodeCreator#setContentAsString(String)}, but sets the
     * content to the specified {@link java.util.Date} instance.
     *
     * @param date
     * @return  a reference to the current node.
     * @throws ParseException
     */
    NodeCreator setContentAsDate(Date date) throws ParseException;

    /**
     * Similar to {@link NodeCreator#setContentAsString(String)}, but sets the
     * content to the specified {@link java.math.BigInteger} instance.
     *
     * @param bigInteger
     * @return  a reference to the current node.
     * @throws ParseException
     */
    NodeCreator setContentAsBigInteger(BigInteger bigInteger) throws ParseException;

    NodeCreator setContentAsBigDecimal(BigDecimal bigDecimal);

    /**
     * Sets the content of the current node to be the specified string. This
     * method does not automatically close the node, but returns a reference to
     * the current node. The caller is responsible for closing the node using
     * {@link NodeCreator#closeEntity()}.
     * <P>
     * Use <code>null</code> to explicitly encode the null value (results in
     * <code>&lt;node xsi:nil="true"/&gt;</code> in XML, while an empty
     * string produces <code>&lt;node&gt;&lt;/node&gt;</code>).
     *
     * @param string    the content for the current node.
     * @return  a reference to the current node.
     * @throws ParseException
     */
    NodeCreator setContentAsString(String string) throws ParseException;

    /**
     * Similar to {@link NodeCreator#setContentAsString(String)}, but sets the
     * content to the specified {@link Boolean} instance.
     *
     * @param bool
     * @return  a reference to the current node.
     * @throws ParseException
     */
    NodeCreator setContentAsBoolean(Boolean bool) throws ParseException;

    /**
     * Similar to {@link NodeCreator#setContentAsString(String)}, but passes the
     * content to the {@link NodeCreator} as a {@link java.io.Reader} instance. Use this
     * to encode large chunks of content in a memory-efficient way.
     *
     * @param data
     * @return  a reference to the current node.
     * @throws java.io.IOException
     * @throws ParseException
     */
    NodeCreator setContent(Reader data) throws IOException, ParseException;

    /**
     * Adds an attribute to the current node.
     *
     * @param key
     * @param value
     * @throws ParseException
     */
    NodeCreator addAttribute(String key, String value) throws ParseException;
}
