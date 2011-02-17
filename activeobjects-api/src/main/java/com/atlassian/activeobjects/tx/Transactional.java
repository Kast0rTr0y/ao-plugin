package com.atlassian.activeobjects.tx;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Annotated methods of an interface or the type itself will make those methods run within a transaction
 * provided by the host application.</p>
 * <p><strong>Note</strong> that in order for this annotation to be processed, one must declare the
 * {@link com.atlassian.activeobjects.external.TransactionalAnnotationProcessor} as a component within their plugin.</p>
 * @see com.atlassian.activeobjects.external.TransactionalAnnotationProcessor
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Inherited
@Documented
public @interface Transactional
{
}
