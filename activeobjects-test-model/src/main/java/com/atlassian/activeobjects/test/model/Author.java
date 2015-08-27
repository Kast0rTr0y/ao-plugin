package com.atlassian.activeobjects.test.model;

import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("LongNameToAuthor")
public interface Author extends Entity {
    @StringLength(60)
    String getName();

    @StringLength(60)
    void setName(String name);

    @ManyToMany(Authorship.class)
    Book[] getBooks();
}
