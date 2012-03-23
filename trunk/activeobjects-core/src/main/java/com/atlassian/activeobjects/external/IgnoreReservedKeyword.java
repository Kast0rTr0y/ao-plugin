package com.atlassian.activeobjects.external;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation is used to ignore the keyword validation done by Active Objects. This should only be used to get a
 * chance to upgrade the plugin so that it uses a different column name in production.</p>
 * <p>See:
 *  <ul>
 *      <li>https://studio.atlassian.com/browse/AO-247</li>
 *      <li>https://studio.atlassian.com/browse/AO-267</li>
 *      <li>https://studio.atlassian.com/browse/AO-278</li>
 *  </ul>
 * </p>
 * @since 0.18.5
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IgnoreReservedKeyword
{
}
