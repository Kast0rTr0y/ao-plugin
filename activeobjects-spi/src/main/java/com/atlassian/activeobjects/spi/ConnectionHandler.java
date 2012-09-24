package com.atlassian.activeobjects.spi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.*;

/**
 * <p>A connection that can't be closed.</p>
 * <p>All calls to Active Objects must happen within a transaction. For this transactions to be successful, we can't let
 * ActiveObjects close the connection in the middle of it.</p>
 */
public final class ConnectionHandler implements InvocationHandler
{
    private final Connection delegate;
    private final Closeable closeable;

    public ConnectionHandler(Connection delegate, Closeable closeable)
    {
        this.delegate = checkNotNull(delegate);
        this.closeable = checkNotNull(closeable);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws SQLException
    {
        if (isCloseMethod(method))
        {
            closeable.close();
            return Void.TYPE;
        }

        return delegate(method, args);
    }

    private Object delegate(Method method, Object[] args) throws SQLException
    {
        try
        {
            return method.invoke(delegate, args);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();

            if (cause instanceof SQLException)
            {
                throw (SQLException) cause;
            }

            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }

            if (cause instanceof Error)
            {
                throw (Error) cause;
            }

            throw new RuntimeException("Unexpected checked exception", cause);
        }
    }

    public static Connection newInstance(Connection c)
    {
        return newInstance(c, new Closeable()
        {
            @Override
            public void close() throws SQLException
            {
            }
        });
    }

    public static Connection newInstance(Connection c, Closeable closeable)
    {
        return (Connection) Proxy.newProxyInstance(
                ConnectionHandler.class.getClassLoader(),
                new Class[]{Connection.class},
                new ConnectionHandler(c, closeable));
    }

    private static boolean isCloseMethod(Method method)
    {
        return method.getName().equals("close")
                && method.getParameterTypes().length == 0;
    }

    public static interface Closeable
    {
        void close() throws SQLException;
    }
}

