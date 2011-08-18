package com.atlassian.activeobjects.test.model;

import net.java.ao.ManyToMany;
import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.SQLType;

import java.sql.Types;

public interface Book extends RawEntity<Long>
{
    @PrimaryKey
    @NotNull
    long getIsbn();

    void setIsbn(long isbn);

    String getTitle();

    void setTitle(String name);

    @SQLType(Types.CLOB)
    String getAbstract();

    @SQLType(Types.CLOB)
    void setAbstract(String bookAbstract);

    double getPrice();

    void setPrice(double price);

    /**
     * Whether this has been read by the user.
     */
    boolean isRead();

    void setRead(boolean read);

    Integer getNumberOfPages();

    void setNumberOfPages(Integer pages);

    @ManyToMany(Authorship.class)
    Author[] getAuthors();
}
