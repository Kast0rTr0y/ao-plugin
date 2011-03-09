package com.atlassian.activeobjects.test.model;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.java.ao.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.*;

public final class Model
{
    private static final String BRIAN_GOETZ = "Brian Goetz";
    private static final String TIM_PEIERLS = "Tim Peierls";
    private static final String JOSHUA_BLOCH = "Joshua Bloch";
    private static final String JOSEPH_BOWBEER = "Joseph Bowbeer";
    private static final String DOUG_LEA = "Doug Lea";
    private static final String DAVID_HOLMES = "David Holmes";
    private static final String MARTIN_ODERSKY = "Martin Odersky";
    private static final String LEX_SPOON = "Lex Spoon";
    private static final String BILL_VENNERS = "Bill Venners";

    private static final String JCIP = "Java Concurrency In Practice";
    private static final double JCIP_PRICE = 37.79;

    private static final String EJ2 = "Effective Java (Second Edition)";
    private static final double EJ2_PRICE = 41.24;

    private static final String PIS = "Programming in Scala";
    private static final double PIS_PRICE = 31.17;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ActiveObjects ao;

    public Model(EntityManager entityManager)
    {
        this.ao = new TestActiveObjects(checkNotNull(entityManager));
    }

    public Model(ActiveObjects ao)
    {
        this.ao = checkNotNull(ao);
    }

    public void migrateEntities()
    {
        ao.migrate(Book.class, Author.class, Authorship.class);
    }

    public void createData()
    {
        logger.info("Adding data to the database!");
        resetDatabase();

        final Author[] jcip = authors(BRIAN_GOETZ, TIM_PEIERLS, JOSHUA_BLOCH, JOSEPH_BOWBEER, DAVID_HOLMES, DOUG_LEA);
        book(JCIP, JCIP_PRICE, jcip);

        final Author[] scala = authors(MARTIN_ODERSKY, LEX_SPOON, BILL_VENNERS);
        book(PIS, PIS_PRICE, scala);

        book(EJ2, EJ2_PRICE, findAuthorWithName(toList(jcip), JOSHUA_BLOCH)); // author is Josh Bloch
    }

    public void emptyDatabase()
    {
        ao.migrate();
    }

    private Author[] authors(String... names)
    {
        return transform(newArrayList(names), new Function<String, Author>()
        {
            public Author apply(String name)
            {
                return author(name);
            }
        }).toArray(new Author[names.length]);
    }

    private Author author(String name)
    {
        final Class<Author> type = Author.class;

        final Author author = ao.create(type);
        author.setName(name);
        author.save();
        return author;
    }

    private Book book(String title, double price, Author... authors)
    {

        final Book book = ao.create(Book.class);
        book.setTitle(title);
        book.setPrice(price);
        book.save();

        for (Author author : authors)
        {
            authorship(book, author);
        }

        return book;
    }

    private void authorship(Book book, Author author)
    {

        final Authorship authorship = ao.create(Authorship.class);
        authorship.setBook(book);
        authorship.setAuthor(author);
        authorship.save();
    }

    private void resetDatabase()
    {
        emptyDatabase();
        migrateEntities();
    }

    public void checkAuthors()
    {
        ImmutableList<Author> authors = allAuthors();
        checkState(9 == authors.size());

        checkState(1 == findAuthorWithName(authors, BRIAN_GOETZ).getBooks().length);
        checkState(1 == findAuthorWithName(authors, TIM_PEIERLS).getBooks().length);
        checkState(2 == findAuthorWithName(authors, JOSHUA_BLOCH).getBooks().length);
        checkState(1 == findAuthorWithName(authors, JOSEPH_BOWBEER).getBooks().length);
        checkState(1 == findAuthorWithName(authors, DOUG_LEA).getBooks().length);
        checkState(1 == findAuthorWithName(authors, DAVID_HOLMES).getBooks().length);
        checkState(1 == findAuthorWithName(authors, MARTIN_ODERSKY).getBooks().length);
        checkState(1 == findAuthorWithName(authors, LEX_SPOON).getBooks().length);
        checkState(1 == findAuthorWithName(authors, BILL_VENNERS).getBooks().length);

        final String me = "Me"; // not a very good author if you ask me

        author(me); // adding a new author

        ao.flushAll();

        authors = allAuthors();
        checkState(10 == authors.size());
        checkState(0 == findAuthorWithName(authors, me).getBooks().length);
    }

    private Author findAuthorWithName(Iterable<Author> authors, final String name)
    {
        return Iterables.find(authors, new Predicate<Author>()
        {
            @Override
            public boolean apply(Author author)
            {
                return name.equals(author.getName());
            }
        });
    }

    private ImmutableList<Author> allAuthors()
    {
        return toList(ao.find(Author.class));
    }

    public void checkBooks()
    {
        final ImmutableList<Book> books = allBooks();

        checkState(3 == books.size());

        checkBook(findBookWithTitle(books, JCIP), JCIP_PRICE, 6);
        checkBook(findBookWithTitle(books, PIS), PIS_PRICE, 3);
        checkBook(findBookWithTitle(books, EJ2), EJ2_PRICE, 1);
    }

    private void checkBook(Book book, double price, int i)
    {
        checkState(price == book.getPrice());
        checkState(i == book.getAuthors().length);
    }

    private Book findBookWithTitle(Iterable<Book> books, final String title)
    {
        return Iterables.find(books, new Predicate<Book>()
        {
            @Override
            public boolean apply(Book book)
            {
                return title.equals(book.getTitle());
            }
        });
    }

    private ImmutableList<Book> allBooks()
    {
        return toList(ao.find(Book.class));
    }

    private <T> ImmutableList<T> toList(T[] authors)
    {
        return ImmutableList.copyOf(newArrayList(authors));
    }
}
