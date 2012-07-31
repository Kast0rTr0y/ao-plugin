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
public final class TransactionalProxy implements InvocationHandler
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
            return invokeInTransaction(method, args);
        }
        else
        {
            return invoke(method, args);
        }
    }

    private Object invokeInTransaction(final Method method, final Object[] args) throws Throwable
    {
        try
        {
            return executeInTransaction(method, args);
        }
        catch (TransactionalException e)
        {
            throw e.getThrowable();
        }
    }

    private Object executeInTransaction(final Method method, final Object[] args)
    {
        return ao.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                try
                {
                    return invoke(method, args);
                }
                catch (IllegalAccessException e)
                {
                    throw new TransactionalException(e);
                }
                catch (InvocationTargetException e)
                {
                    throw new TransactionalException(e);
                }
            }
        });
    }

    private Object invoke(Method method, Object[] args) throws IllegalAccessException, InvocationTargetException
    {
        return method.invoke(obj, args);
    }

    /**
     * Makes the given instance object transactional. It will do so by proxying the object, so one can no longer
     * reference the original object implementation after calling this method.
     *
     * @param ao the {@link com.atlassian.activeobjects.external.ActiveObjects} service to use for transaction management.
     * @param o the object to make transactional.
     * @return a transactional proxy of the object passed as a parameter.
     */
    public static Object transactional(ActiveObjects ao, Object o)
    {
        checkNotNull(o);
        final Class c = o.getClass();
        return Proxy.newProxyInstance(c.getClassLoader(), c.getInterfaces(), new TransactionalProxy(ao, o));
    }

    static boolean isAnnotated(Method method)
    {
        return method != null && (isAnnotationPresent(method) || isAnnotationPresent(method.getDeclaringClass()));
    }

    /**
     * Tells whether the given class is annotated as being transactional. I.e with annotation defined at {@link #ANNOTATION_CLASS}.
     *
     * @param c the class to scan for annotations
     * @return {@code true} if the class is annotated with the defined annotation
     */
    public static boolean isAnnotated(Class c)
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

    private static final class TransactionalException extends RuntimeException
    {
        public TransactionalException(Throwable cause)
        {
            super(cause);
        }

        public Throwable getThrowable()
        {
            final Throwable cause = getCause();
            if (cause instanceof InvocationTargetException)
            {
                return cause.getCause();
            }
            else
            {
                return cause;
            }
        }
    }
}
