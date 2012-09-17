package com.atlassian.activeobjects.spi;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>Unit tests for {@link ConnectionHandler}.</p>
 */
public class ConnectionHandlerTest {


    @Rule public ExpectedException exception = ExpectedException.none();

    /**
     * <p>Test that public methods can be invoked when the delegate is an instance of a private class.</p>
     *
     * @see <a href="https://studio.atlassian.com/browse/AO-329">AO-329</a>
     */
    @Test
    public void testPrivateDelegate() throws Exception {
        ConnectionHandler.newInstance(PrivateConnectionFactory.newPrivateConnection()).createStatement();
    }


    /**
     *
     * @see <a href="https://ecosystem.atlassian.net/browse/AO-364">AO-364</a>
     * @throws Exception because it's a test :P
     */
    @Test
    public void testIfExceptionsAreHandledCorrectly() throws Exception
    {
        final Connection connectionMock = mock(Connection.class);
        final ConnectionHandler.Closeable closeableMock = mock(ConnectionHandler.Closeable.class);

        final SQLException expectedException = new SQLException("An exception that should be re-thrown by proxy object");
        when(connectionMock.prepareStatement(any(String.class))).thenThrow(expectedException);
        final Connection connectionHandler = ConnectionHandler.newInstance(connectionMock, closeableMock);
        exception.expect(SQLException.class);
        exception.expectMessage(CoreMatchers.equalTo("An exception that should be re-thrown by proxy object"));
        connectionHandler.prepareStatement("this call should throw SQLException");
    }

}
