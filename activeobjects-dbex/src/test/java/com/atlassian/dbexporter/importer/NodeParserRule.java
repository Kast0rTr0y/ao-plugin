package com.atlassian.dbexporter.importer;

import com.atlassian.dbexporter.node.NodeParser;
import com.atlassian.dbexporter.node.NodeStreamReader;
import com.atlassian.dbexporter.node.stax.StaxStreamReader;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * This is convenient rule for JUnit 4 tests, it will initialise a node parser for tests annotated with the {@link Xml}
 * annotation.
 *
 * Here is an example of how to use it:
 * <code><pre>
 * public class MyTest {
 *   &#64;Rule
 *   public NodeParserRule rule = new NodeParserRule();
 *
 *   &#64;Test
 *   &#64;Xml("&lt;somexml /&gt;")
 *   public void aTestWithXml() {
 *     NodeParser node = rule.getNode();
 *     ...
 *   }
 * }
 * </pre></code>
 */
public final class NodeParserRule implements MethodRule
{
    private NodeStreamReader streamReader;
    private NodeParser node;

    public NodeParser getNode()
    {
        return node;
    }

    public Statement apply(final Statement statement, final FrameworkMethod frameworkMethod, final Object o)
    {
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                before(frameworkMethod);
                try
                {
                    statement.evaluate();
                }
                finally
                {
                    after();
                }
            }
        };
    }

    private void before(FrameworkMethod method)
    {
        final Xml xml = method.getAnnotation(Xml.class);
        if (xml != null)
        {
            streamReader = new StaxStreamReader(new StringReader(xml.value()));
            node = streamReader.getRootNode();
            assertFalse(node.isClosed());
        }
    }

    private void after()
    {
        if (streamReader != null)
        {
            streamReader.close();
        }
        streamReader = null;
        node = null;
    }
}
