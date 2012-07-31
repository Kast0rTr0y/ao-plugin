package com.atlassian.activeobjects.junit;

import java.lang.annotation.Documented;

@Documented
//@Retention(RUNTIME)
//@Target(TYPE)
//@Inherited
public @interface PackageVersion
{
    String value();

    String version();
}
