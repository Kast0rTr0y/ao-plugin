package com.atlassian.dbexporter;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.LinkedList;

import static com.google.common.base.Predicates.*;
import static java.util.Arrays.asList;

public final class Context
{
    private final Collection<Object> context = new LinkedList<Object>();

    public Context(Object... objects)
    {
        context.addAll(asList(objects));
    }

    /**
     * Gets the single element of the given type in the import context, returning {@code null} if none is present.
     *
     * @param type the type of the element to look for
     * @param <T> the type parameter of the type to look for
     * @return the element if it exists, {@code null} otherwise
     * @throws IllegalStateException if more than one element of the given type are present in the context. If you need
     * to get multiple elements of type {@code T} use the {@link #getAll(Class)} method.
     * @see #getAll(Class)
     */
    public <T> T get(Class<T> type)
    {
        final Collection<T> zeroOrOne = getZeroOrOneElement(type);
        return zeroOrOne.isEmpty() ? null : first(zeroOrOne);
    }

    /**
     * Gets all the element of the given type in the import context
     *
     * @param type the type of the elements to look for
     * @param <T> the type parameter of the type to look for
     * @return all the elements of type {@code T}, note that there might be none.
     * @see #get(Class)
     */
    public <T> Collection<T> getAll(Class<T> type)
    {
        return filter(type);
    }

    /**
     * Add the given object to the context
     *
     * @param obj the new object in the context
     * @return {@code this}
     */
    public Context put(Object obj)
    {
        context.add(obj);
        return this;
    }

    /**
     * Add all the given objects to the context
     *
     * @param objs the collection of objects to add to the context
     * @return {@code this}
     */
    public Context putAll(Collection<?> objs)
    {
        context.addAll(objs);
        return this;
    }

    private <T> Collection<T> getZeroOrOneElement(Class<T> type)
    {
        return checkZeroOrOneElement(type, getAll(type));
    }

    private <T> Collection<T> checkZeroOrOneElement(Class<T> type, Collection<T> c)
    {
        if (c.size() > 1)
        {
            throw new IllegalStateException("Found more than one element of type " + type.getName() + " in import context!");
        }
        return c;
    }

    @SuppressWarnings("unchecked")
    private <T> Collection<T> filter(Class<T> type)
    {
        return (Collection<T>) Collections2.filter(context, instanceOf(type));
    }

    private static <T> T first(Iterable<T> c)
    {
        return Iterables.get(c, 0);
    }
}
