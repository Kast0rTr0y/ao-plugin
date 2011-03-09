package com.atlassian.dbexporter.node;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * Represents a node in streaming XML documents. This is a small abstraction
 * layer over StAX that allows other streaming data formats to be used (JSON,
 * ASN.1, ?) and make it easier to deal with StAX, of which the API can be a bit
 * tedious. This interface also provides type conversion instead of just strings.
 * This interface only provides read access to a streaming node graph.
 *
 * @see NodeCreator counterpart of this interface that provides the ability to
 * write node graphs.
 * @see NodeStreamReader
 * @author Erik van Zijst
 */
public interface NodeParser {

    /**
     * Retrieves the value of an attribute of the current node. If the attribute
     * does not exist, <code>null</code> is returned.
     *
     * @param key   the name of the attribute.
     * @return  the value of the attribute, or <code>null</code> if the attribute
     * does not exist.
     * @throws ParseException   when the input could not be parsed.
     * @throws IllegalStateException when this method was called after one of
     * the <code>getContent...</code>, or the {@link NodeParser#getNextNode()}
     * method was called.
     */
    String getAttribute(String key) throws ParseException, IllegalStateException;

    /**
     * Retrieves the value of an attribute of the current node. If the attribute
     * does not exist, a {@link ParseException} is thrown.
     *
     * @param key   the name of the attribute.
     * @return  the value of the attribute (can be an empty string, or even
     * <code>null</code> if the attribute is present, but has no value - think
     * <code>xsi:nil="true"</code>).
     * @throws ParseException   when the input could not be parsed.
     * @throws IllegalStateException when this method was called after one of
     * the <code>getContent...</code>, or the {@link NodeParser#getNextNode()}
     * method was called.
     */
    String getRequiredAttribute(String key) throws ParseException, IllegalStateException;

    /**
     * Returns the name of the current entity.
     *
     * @return  the name of the current entity.
     * @throws ParseException   when the input could not be parsed.
     */
    String getName() throws ParseException;

    /**
     * Returns <code>true</code> if this instance represents the end of a node
     * (closing tag in XML).
     * If this is the case, {@link NodeParser#getNextNode()} will return a
     * reference to the parent node. Also, when the end of a node is reached,
     * {@link NodeParser#getAttribute(String)}, {@link NodeParser#getRequiredAttribute(String)}
     * and the various getContent() methods can no longer be invoked. Doing so
     * will raise a {@link ParseException}.
     *
     * @return  <code>true</code> if this instance represents the end of a node.
     * @throws ParseException   when the input could not be parsed.
     */
    boolean isClosed() throws ParseException;

    /**
     * Returns the next node in the graph. This could be:
     *
     * <LI>a child node (in which case {@link NodeParser#isClosed()} will be
     * <code>false</code> on the returned instance);</LI>
     * <LI>the close tag of the current node ({@link NodeParser#isClosed()}
     * will be <code>true</code> on the returned instance;</LI>
     * <LI>the end of the document ({@link NodeParser#isClosed()} is
     * <code>true</code> on the current instance), then <code>null</code> will
     * be returned.</LI>
     *
     * @return  the next node in the graph.
     * @throws ParseException   when the input could not be parsed.
     */
    NodeParser getNextNode() throws ParseException;

    /**
     * Returns the content of the current node as a string. A node either
     * contains content, child nodes, or nothing at all. If the current node
     * does not contain content, a {@link ParseException} is thrown. When the
     * current node is a content node, but contains no data (e.g.
     * <code><node xsi:nil="true"/></code> in XML), <code>null</code> is returned.
     * <P>
     * When this method returns, the current node will be closed. Calling this
     * method again will raise an {@link IllegalStateException}.
     *
     * @return  the content of the current node.
     * @throws ParseException   when the current node is not a content node, or
     * when the input could not be parsed.
     * @throws IllegalStateException    if the current node cannot contain
     * content (for instance because {@link NodeParser#isClosed()} is true.
     */
    String getContentAsString() throws ParseException, IllegalStateException;

    /**
     * Similar to {@link NodeParser#getContentAsString()}, but converts content
     * to a boolean value.
     *
     * @return  the content of the current node as a boolean value.
     * @throws ParseException   when the current node is not a content node, or
     * when the input could not be parsed.
     * @throws IllegalStateException    if the current node cannot contain
     * content (for instance because {@link NodeParser#isClosed()} is true.
     */
    Boolean getContentAsBoolean() throws ParseException, IllegalStateException;

    /**
     * Similar to {@link NodeParser#getContentAsString()}, but converts content
     * to a {@link java.util.Date} instance.
     *
     * @return  the content of the current node as a {@link java.util.Date} instance.
     * @throws ParseException   when the current node is not a content node, or
     * when the input could not be parsed, or when the content does not conform
     * to the standardized date format.
     * @throws IllegalStateException    if the current node cannot contain
     * content (for instance because {@link NodeParser#isClosed()} is true.
     */
    Date getContentAsDate() throws ParseException, IllegalStateException;

    /**
     * Similar to {@link NodeParser#getContentAsString()}, but converts content
     * to a {@link java.math.BigInteger} instance.
     *
     * @return  the content of the current node as a {@link java.math.BigInteger} instance.
     * @throws ParseException   when the current node is not a content node, or
     * when the input could not be parsed, or the content does not contain a
     * numeric integer value.
     * @throws IllegalStateException    if the current node cannot contain
     * content (for instance because {@link NodeParser#isClosed()} is true.
     */
    BigInteger getContentAsBigInteger() throws ParseException, IllegalStateException;

    BigDecimal getContentAsBigDecimal() throws ParseException, IllegalStateException;

    /**
     * Similar to {@link NodeParser#getContentAsString()}, but writes the contents
     * to the specified {@link java.io.Writer} instance. Useful for reading large lumps
     * of content in a memory-efficient way. The writer is not closed.
     *
     * @param writer
     * @throws ParseException   when the current node is not a content node, or
     * when the input could not be parsed.
     * @throws IllegalStateException    if the current node cannot contain
     * content (for instance because {@link NodeParser#isClosed()} is true.
     */
    void getContent(Writer writer) throws ParseException, IllegalStateException;
}
