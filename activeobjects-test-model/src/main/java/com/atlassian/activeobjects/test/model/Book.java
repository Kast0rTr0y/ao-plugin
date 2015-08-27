package com.atlassian.activeobjects.test.model;

import net.java.ao.Accessor;
import net.java.ao.ManyToMany;
import net.java.ao.Mutator;
import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;

import java.util.Date;

public interface Book extends RawEntity<Long> {
    @PrimaryKey
    @NotNull
    long getIsbn();

    void setIsbn(long isbn);

    @StringLength(255)
    String getTitle();

    @StringLength(255)
    void setTitle(String name);

    @StringLength(StringLength.UNLIMITED)
    String getAbstract();

    @StringLength(StringLength.UNLIMITED)
    void setAbstract(String bookAbstract);

    double getPrice();

    void setPrice(double price);

    /**
     * Whether this has been read by the user.
     */
    @Accessor("IS_READ")
    boolean isRead();

    @Mutator("IS_READ")
    void setRead(boolean read);

    Date getPublished();

    void setPublished(Date date);

    Integer getNumberOfPages();

    void setNumberOfPages(Integer pages);

    @ManyToMany(Authorship.class)
    Author[] getAuthors();
}
