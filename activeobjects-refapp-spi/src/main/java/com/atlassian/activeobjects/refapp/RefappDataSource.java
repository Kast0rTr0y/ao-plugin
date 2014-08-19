package com.atlassian.activeobjects.refapp;

import com.atlassian.refapp.api.ConnectionProvider;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import javax.sql.DataSource;

import static com.google.common.base.Preconditions.checkNotNull;

public final class RefappDataSource implements DataSource
{
    private final ConnectionProvider connectionProvider;

    public RefappDataSource(final ConnectionProvider connectionProvider)
    {
        this.connectionProvider = checkNotNull(connectionProvider);
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return connectionProvider.connection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException
    {
        return connectionProvider.connection();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setLogWriter(final PrintWriter out) throws SQLException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setLoginTimeout(final int seconds) throws SQLException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    // @Override // Java 7 only
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        throw new UnsupportedOperationException("Not implemented");
    }
}
