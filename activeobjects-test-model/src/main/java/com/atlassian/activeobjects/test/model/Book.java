package com.atlassian.activeobjects.test.model;

import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.OneToMany;

public interface Book extends Entity
{
    String getTitle();

    void setTitle(String name);

    double getPrice();
    void setPrice(double price);

    @ManyToMany(Authorship.class)
    Author[] getAuthors();
}
