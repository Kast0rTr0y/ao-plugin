package com.atlassian.activeobjects.external;

import net.java.ao.Transaction;
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
 *   &lt;component key="tx-annotation-processor" class="com.atlassian.activeobjects.external.TransactionalAnnotationProcessor" /&gt;
 * </code>
 * <p><strong>Note:</strong> This class does not live in the {@link com.atlassian.activeobjects.tx} package in order to
 * be available to plugins without any additional imports (OSGi wise).</p>
 * @see com.atlassian.activeobjects.tx.Transactional
 */
public final class TransactionalAnnotationProcessor implements BeanPostProcessor
{
    private final ActiveObjects ao;

    public TransactionalAnnotationProcessor(ActiveObjects ao)
    {
        // AO-283, http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6588239
        // prevent a sun (Oracle) JVM deadlock.
        Transaction.class.getAnnotations();


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
