package it.com.atlassian.activeobjects;

/**
 * A simple interface for an Active Objects service consumer.
 *
 * @see com.atlassian.activeobjects.test.Plugins#newConsumerPlugin(String)
 */
public interface ActiveObjectsTestConsumer
{
    Object run() throws Exception;
}
