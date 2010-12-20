package com.atlassian.activeobjects.testplugin.model;

import net.java.ao.Entity;

public interface Authorship extends Entity
{
    public Book getBook();
    public void setBook(Book book);

    public Author getAuthor();
    public void setAuthor(Author author);
}
