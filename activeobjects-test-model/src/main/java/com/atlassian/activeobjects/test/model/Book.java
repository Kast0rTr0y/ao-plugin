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

    long getIsbn();
    void setIsbn(long isbn);

    /**
     * Whether this has been read by the user.
     */
    boolean isRead();
    void setRead(boolean read);

    @ManyToMany(Authorship.class)
    Author[] getAuthors();
}
