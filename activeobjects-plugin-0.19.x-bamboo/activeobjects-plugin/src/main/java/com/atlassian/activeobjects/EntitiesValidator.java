package com.atlassian.activeobjects;

import net.java.ao.RawEntity;
import net.java.ao.schema.NameConverters;

import java.util.Set;

public interface EntitiesValidator
{
    Set<Class<? extends RawEntity<?>>> check(Set<Class<? extends RawEntity<?>>> entityClasses, NameConverters nameConverters);
}
