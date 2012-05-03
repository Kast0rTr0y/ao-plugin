package com.atlassian.activeobjects.spi;

import org.junit.Test;

import java.sql.*;
import java.util.Map;
import java.util.Properties;

/**
 * <p>Unit tests for {@link ConnectionHandler}.</p>
 */
public class ConnectionHandlerTest {

    /**
     * <p>Test that public methods can be invoked when the delegate is an instance of a private class.</p>
     *
     * @see <a href="https://studio.atlassian.com/browse/AO-329">AO-329</a>
     */
    @Test
    public void testPrivateDelegate() throws Exception {
        ConnectionHandler.newInstance(PrivateConnectionFactory.newPrivateConnection()).createStatement();
    }

}
