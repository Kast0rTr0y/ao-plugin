package com.atlassian.activeobjects.tx;

import com.atlassian.activeobjects.external.TransactionalAnnotationProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Annotating methods of an interface with this annotation will make those methods run within a transaction
 * provided by the host application.</p>
 * <p><strong>Note</strong> that in order for this annotation to be processed, one must declare the
 * {@link TransactionalAnnotationProcessor} as a component within their plugin.
 * This processor is a {@link BeanPostProcessor} which will only be able to handle classes instanciated as a
 * <a href="http://confluence.atlassian.com/display/PLUGINFRAMEWORK/Component+Plugin+Module">components</a>.</p>
 * @see TransactionalAnnotationProcessor
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Inherited
@Documented
public @interface Transactional
{
}
