package com.atlassian.activeobjects.internal;

import com.atlassian.activeobjects.external.ActiveObjects;
import net.java.ao.RawEntity;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service factory for providing pooled {@link com.atlassian.activeobjects.external.ActiveObjects} instances.
 */
public class ActiveObjectsServiceFactory implements ServiceFactory
{
    private volatile ActiveObjectsProvider activeObjectsProvider;
    private final Map<Bundle, ProxyRegistration> activeObjectProxies;

    public ActiveObjectsServiceFactory()
    {
        activeObjectProxies = new HashMap<Bundle, ProxyRegistration>();
    }

    void init(ActiveObjectsProvider activeObjectsProvider)
    {
        this.activeObjectsProvider = activeObjectsProvider;
        for (ProxyRegistration reg : activeObjectProxies.values())
        {
            reg.getProxy().start(this.activeObjectsProvider);
        }
    }

    void stop()
    {
        for (ProxyRegistration reg : activeObjectProxies.values())
        {
            reg.getProxy().stop();
        }
        this.activeObjectsProvider = null;
    }

    void destroy()
    {
        stop();
        activeObjectProxies.clear();
    }

    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        String prefix = bundle.getSymbolicName().substring(0, Math.min(4, bundle.getSymbolicName().length()));
        prefix += bundle.getSymbolicName().hashCode();

        ActiveObjectsProxy proxy = new ActiveObjectsProxy(prefix, activeObjectsProvider);
        activeObjectProxies.put(bundle, new ProxyRegistration(prefix, proxy));
        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ActiveObjects.class}, proxy);
    }

    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object o)
    {
        ProxyRegistration reg = activeObjectProxies.remove(bundle);
        reg.getProxy().stop();
    }

    private static class ProxyRegistration
    {
        private final ActiveObjectsProxy proxy;
        private final String key;

        public ProxyRegistration(String key, ActiveObjectsProxy proxy)
        {
            this.key = key;
            this.proxy = proxy;
        }

        public ActiveObjectsProxy getProxy()
        {
            return proxy;
        }

        public String getKey()
        {
            return key;
        }
    }

    private static class ActiveObjectsProxy implements InvocationHandler
    {
        private volatile ActiveObjects delegate;
        private volatile Class<? extends RawEntity<?>>[] migrateArgs;

        private final String key;
        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private final Lock read = readWriteLock.readLock();
        private final Lock write = readWriteLock.writeLock();

        public ActiveObjectsProxy(String key, ActiveObjectsProvider provider)
        {
            this.key = key;
            if (provider != null)
            {
                start(provider);
            }
        }

        public void stop()
        {
            write.lock();
            try
            {
                delegate = null;
            }
            finally
            {
                write.unlock();
            }
        }

        public void start(ActiveObjectsProvider provider)
        {
            write.lock();
            try
            {
                delegate = provider.createActiveObjects(key);
                if (migrateArgs != null) // then we need to actually to the "migration" now that we're ready
                {
                    try
                    {
                        delegate.migrate(migrateArgs);
                    }
                    catch (SQLException e)
                    {
                        stop();
                        throw new IllegalStateException(e);
                    }
                }
            }
            finally
            {
                write.unlock();
            }
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            read.lock();
            try
            {
                if (isMigrateMethod(method))
                {
                    migrateArgs = (Class[]) args[0];
                }

                if (delegate != null)
                {
                    return method.invoke(delegate, args);
                }
                else
                {
                    if (isMigrateMethod(method))
                    {
                        return null; // we'll call the migrate method on start
                    }

                    int triesLeft = 10;
                    while (triesLeft-- > 0)
                    {
                        System.out.println("try " + triesLeft);
                        Thread.sleep(500);
                        if (delegate != null)
                        {
                            return method.invoke(delegate, args);
                        }
                    }
                    throw new RuntimeException("Service not available");
                }
            }
            finally
            {
                read.unlock();
            }
        }

        private boolean isMigrateMethod(Method method)
        {
            return method.getName().equals("migrate");
        }
    }

}
