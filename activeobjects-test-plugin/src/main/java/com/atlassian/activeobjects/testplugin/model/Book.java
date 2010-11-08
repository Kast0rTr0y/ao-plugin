package com.atlassian.activeobjects.testplugin.model;

import net.java.ao.Entity;

public interface Book extends Entity
{
    String getTitle();

    void setTitle(String name);
}
