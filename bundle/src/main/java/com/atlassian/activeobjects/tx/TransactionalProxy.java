package com.atlassian.activeobjects.tx;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.sal.api.transaction.TransactionCallback;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The proxy that takes care of wrapping annotated methods within a transaction.
 */
final class TransactionalProxy implements InvocationHandler
{
    private static final Class<? extends Annotation> ANNOTATION_CLASS = Transactional.class;

    private final ActiveObjects ao;
    private final Object obj;

    public TransactionalProxy(ActiveObjects ao, Object obj)
    {
        this.ao = ao;
        this.obj = obj;
    }

    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable
    {
        if (isAnnotated(method))
        {
            return ao.executeInTransaction(new TransactionCallback<Object>()
            {
                public Object doInTransaction()
                {
                    try
                    {
                        return method.invoke(obj, args);
                    }
                    catch (IllegalAccessException e)
                    {
                        throw new RuntimeException(e);
                    }
                    catch (InvocationTargetException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        else
        {
            return method.invoke(obj, args);
        }
    }

    static Object transactional(ActiveObjects ao, Object o)
    {
        checkNotNull(o);
        final Class c = o.getClass();
        return Proxy.newProxyInstance(c.getClassLoader(), c.getInterfaces(), new TransactionalProxy(ao, o));
    }

    static boolean isAnnotated(Method method)
    {
        return method != null && (isAnnotationPresent(method) || isAnnotationPresent(method.getDeclaringClass()));
    }

    static boolean isAnnotated(Class c)
    {
        if (c != null)
        {
            if (c.isInterface())
            {
                if (isAnnotationPresent(c))
                {
                    return true;
                }
                for (Method method : c.getMethods())
                {
                    if (isAnnotated(method))
                    {
                        return true;
                    }
                }
            }

            for (Class ifce : c.getInterfaces())
            {
                if (isAnnotated(ifce))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAnnotationPresent(AnnotatedElement e)
    {
        return e.isAnnotationPresent(ANNOTATION_CLASS);
    }
}
