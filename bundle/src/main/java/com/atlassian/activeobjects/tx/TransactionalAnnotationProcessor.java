package com.atlassian.activeobjects.tx;

import com.atlassian.activeobjects.external.ActiveObjects;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import static com.atlassian.activeobjects.tx.TransactionalProxy.isAnnotated;
import static com.atlassian.activeobjects.tx.TransactionalProxy.transactional;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>This is the class that processes the {@link com.atlassian.activeobjects.tx.Transactional} annotation
 * within a plugin.</p>
 * <p>Simply add this snippet of code in your plugin descriptor:</p>
 * <code>
 *   &lt;component key="tx-annotation-processor" class="com.atlassian.activeobjects.tx.TransactionalAnnotationProcessor" /&gt;
 * </code>
 * @see com.atlassian.activeobjects.tx.Transactional
 */
public final class TransactionalAnnotationProcessor implements BeanPostProcessor
{
    private final ActiveObjects ao;

    public TransactionalAnnotationProcessor(ActiveObjects ao)
    {
        this.ao = checkNotNull(ao);
    }

    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException
    {
        return o;
    }

    public Object postProcessAfterInitialization(Object o, String s) throws BeansException
    {
        return isAnnotated(o.getClass()) ? transactional(ao, o) : o;
    }
}
