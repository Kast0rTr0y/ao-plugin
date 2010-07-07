package com.atlassian.activeobjects.internal;

import net.java.ao.DatabaseProvider;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * This makes me want to cringe and gouge my eyes out. Sorry guys.
 * @author NeverGoingToAdmitToIt
 */
@Deprecated
public class ProviderDataSourceProxy implements InvocationHandler {

    private DatabaseProvider delegate;
    private DataSource dataSource;

    public ProviderDataSourceProxy(DatabaseProvider delegate, DataSource dataSource) {
        this.delegate = delegate;
        this.dataSource = dataSource;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("getConnectionImpl")) {
            return dataSource.getConnection();
        }

        Class<? extends DatabaseProvider> clazz = delegate.getClass();
        Method method2 = clazz.getMethod(method.getName(), method.getParameterTypes());
        method2.setAccessible(true);

        return method2.invoke(delegate, args);
    }

    public static DatabaseProvider newInstance(DatabaseProvider delegate, DataSource dataSource) {
        return (DatabaseProvider) Proxy.newProxyInstance(ProviderDataSourceProxy.class.getClassLoader(),
                new Class[] {DatabaseProvider.class}, new ProviderDataSourceProxy(delegate, dataSource));
    }
}
