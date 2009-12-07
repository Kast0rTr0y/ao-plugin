package com.atlassian.labs.activeobjects.internal;

import org.osgi.framework.ServiceFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import com.atlassian.labs.activeobjects.internal.ActiveObjectsProvider;
import com.atlassian.labs.activeobjects.external.ActiveObjects;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

/**
 * Service factory for providing pooled {@link com.atlassian.labs.activeobjects.external.ActiveObjects} instances.
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
        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {ActiveObjects.class}, proxy);
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

        public ProxyRegistration(String key, ActiveObjectsProxy proxy) {
            this.key = key;
            this.proxy = proxy;
        }

        public ActiveObjectsProxy getProxy() {
            return proxy;
        }

        public String getKey() {
            return key;
        }
    }

    private static class ActiveObjectsProxy implements InvocationHandler
    {
        private volatile ActiveObjects delegate;
        private final String key;
        private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private final Lock read  = readWriteLock.readLock();
        private final Lock write = readWriteLock.writeLock();

        public ActiveObjectsProxy(String key, ActiveObjectsProvider provider) {
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
            }
            finally
            {
                write.unlock();
            }


        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            read.lock();
            try
            {
                if (delegate != null)
                {
                    return method.invoke(delegate, args);
                }
                else
                {
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
    }

}
