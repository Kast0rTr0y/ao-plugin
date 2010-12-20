package com.atlassian.activeobjects.testplugin.model;

import net.java.ao.Entity;
import net.java.ao.ManyToMany;

public interface Author extends Entity
{
    String getName();
    void setName(String name);

    @ManyToMany(Authorship.class)
    Book[] getBooks();
}
