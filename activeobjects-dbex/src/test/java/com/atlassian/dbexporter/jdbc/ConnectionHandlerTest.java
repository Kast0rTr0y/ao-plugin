package com.atlassian.dbexporter.jdbc;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class ConnectionHandlerTest
{
    @Test(expected = SQLException.class)
    public void testIfExceptionsAreHandledCorrectly() throws Exception
    {
        final Connection connectionMock = mock(Connection.class);
        final ConnectionHandler.Closeable closeableMock = mock(ConnectionHandler.Closeable.class);

        final SQLException expectedException = new SQLException("An exception that should be re-thrown by proxy object");
        when(connectionMock.prepareStatement(any(String.class))).thenThrow(expectedException);
        final Connection connectionHandler = ConnectionHandler.newInstance(connectionMock, closeableMock);

        try
        {
            connectionHandler.prepareStatement("this call should throw SQLException");
        }
        catch (SQLException e)
        {
            Assert.assertEquals(e, expectedException);
            verify(connectionMock).prepareStatement(any(String.class));
            throw e;
        }
    }

}