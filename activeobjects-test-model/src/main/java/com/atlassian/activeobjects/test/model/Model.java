package com.atlassian.activeobjects.test.model;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;

public final class Model {
    private static final String DATE_FORMAT = "MMM d, yyyy";

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
    private static final Date JCIP_PUBLISHED = toDate("May 19, 2006");
    private static final long JCIP_ISBN = 9780321349606L;
    private static final boolean JCIP_READ = true;
    private static final Integer JCIP_PAGES = 403;
    private static final Supplier<String> JCIP_ABSTRACT = Suppliers.memoize(new BookAbstractSupplier("/com/atlassian/activeobjects/test/model/jcip.txt"));
    private static final String JCIP_COVER = "/com/atlassian/activeobjects/test/model/jcip.jpg";

    private static final String EJ2 = "Effective Java (Second Edition)";
    private static final double EJ2_PRICE = 41.24;
    private static final Date EJ2_PUBLISHED = toDate("May 28, 2008");
    private static final long EJ2_ISBN = 9780321356680L;
    private static final boolean EJ2_READ = false;
    private static final Integer EJ2_PAGES = null;
    private static final Supplier<String> EJ2_ABSTRACT = Suppliers.memoize(new BookAbstractSupplier("/com/atlassian/activeobjects/test/model/ej2.txt"));
    private static final String EJ2_COVER = "/com/atlassian/activeobjects/test/model/ej2.jpg";

    private static final String PIS = "Programming in Scala";
    private static final double PIS_PRICE = 31.17;
    private static final Date PIS_PUBLISHED = toDate("Jan 4, 2011");
    private static final long PIS_ISBN = 9780981531601L;
    private static final boolean PIS_READ = true;
    private static final Integer PIS_PAGES = null;
    private static final Supplier<String> PIS_ABSTRACT = Suppliers.memoize(new BookAbstractSupplier("/com/atlassian/activeobjects/test/model/pis.txt"));
    private static final String PIS_COVER = "/com/atlassian/activeobjects/test/model/pis.jpg";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ActiveObjects ao;

    public Model(EntityManager entityManager) {
        this.ao = new TestActiveObjects(checkNotNull(entityManager));
    }

    public Model(ActiveObjects ao) {
        this.ao = checkNotNull(ao);
    }

    public void emptyDatabase() {
        logger.debug("Emptying the database!");
        ao.migrateDestructively();
    }

    public void migrateEntities() {
        final Class<? extends RawEntity<?>>[] entities = new Class[]{Book.class, Author.class, Authorship.class};
        logger.debug("Migrating entities ({}), this will create tables", Joiner.on(',').join(entities));
        ao.migrate(entities);
    }

    public void createData() {
        logger.info("Adding data to the database!");
        resetDatabase();

        final Author[] jcip = authors(BRIAN_GOETZ, TIM_PEIERLS, JOSHUA_BLOCH, JOSEPH_BOWBEER, DAVID_HOLMES, DOUG_LEA);
        book(JCIP, JCIP_PRICE, JCIP_ISBN, JCIP_PUBLISHED, JCIP_READ, JCIP_PAGES, JCIP_ABSTRACT.get(), jcip);

        final Author[] scala = authors(MARTIN_ODERSKY, LEX_SPOON, BILL_VENNERS);
        book(PIS, PIS_PRICE, PIS_ISBN, PIS_PUBLISHED, PIS_READ, PIS_PAGES, PIS_ABSTRACT.get(), scala);

        book(EJ2, EJ2_PRICE, EJ2_ISBN, EJ2_PUBLISHED, EJ2_READ, EJ2_PAGES, EJ2_ABSTRACT.get(), findAuthorWithName(toList(jcip), JOSHUA_BLOCH)); // author is Josh Bloch
    }

    private Author[] authors(String... names) {
        return transform(newArrayList(names), new Function<String, Author>() {
            public Author apply(String name) {
                return author(name);
            }
        }).toArray(new Author[names.length]);
    }

    private Author author(String name) {
        final Class<Author> type = Author.class;

        final Author author = ao.create(type);
        author.setName(name);
        author.save();
        return author;
    }

    private Book book(String title, double price, long isbn, Date published, boolean read, Integer pages,
                      String bookAbstract, Author... authors) {
        final Book book = ao.create(Book.class, ImmutableMap.<String, Object>of("ISBN", isbn));
        book.setTitle(title);
        book.setAbstract(bookAbstract);
        book.setPrice(price);
        book.setRead(read);
        book.setNumberOfPages(pages);
        book.setPublished(published);
        book.save();

        for (Author author : authors) {
            authorship(book, author);
        }

        return book;
    }

    private void authorship(Book book, Author author) {

        final Authorship authorship = ao.create(Authorship.class);
        authorship.setBook(book);
        authorship.setAuthor(author);
        authorship.save();
    }

    private void resetDatabase() {
        emptyDatabase();
        migrateEntities();
    }

    public void checkAuthors() {
        ImmutableList<Author> authors = allAuthors();
        assertEquals(9, authors.size());

        assertEquals(1, findAuthorWithName(authors, BRIAN_GOETZ).getBooks().length);
        assertEquals(1, findAuthorWithName(authors, TIM_PEIERLS).getBooks().length);
        assertEquals(2, findAuthorWithName(authors, JOSHUA_BLOCH).getBooks().length);
        assertEquals(1, findAuthorWithName(authors, JOSEPH_BOWBEER).getBooks().length);
        assertEquals(1, findAuthorWithName(authors, DOUG_LEA).getBooks().length);
        assertEquals(1, findAuthorWithName(authors, DAVID_HOLMES).getBooks().length);
        assertEquals(1, findAuthorWithName(authors, MARTIN_ODERSKY).getBooks().length);
        assertEquals(1, findAuthorWithName(authors, LEX_SPOON).getBooks().length);
        assertEquals(1, findAuthorWithName(authors, BILL_VENNERS).getBooks().length);

        final String me = "Me"; // not a very good author if you ask me

        author(me); // adding a new author

        ao.flushAll();

        authors = allAuthors();
        assertEquals(10, authors.size());
        assertEquals(0, findAuthorWithName(authors, me).getBooks().length);
    }

    private Author findAuthorWithName(Iterable<Author> authors, final String name) {
        return Iterables.find(authors, new Predicate<Author>() {
            @Override
            public boolean apply(Author author) {
                return name.equals(author.getName());
            }
        });
    }

    private ImmutableList<Author> allAuthors() {
        return toList(ao.find(Author.class));
    }

    public void checkBooks() {
        final ImmutableList<Book> books = allBooks();

        assertEquals(3, books.size());

        checkBook(findBookWithTitle(books, JCIP), JCIP_ABSTRACT.get(), JCIP_PRICE, JCIP_ISBN, JCIP_PUBLISHED, JCIP_READ, JCIP_PAGES, 6);
        checkBook(findBookWithTitle(books, PIS), PIS_ABSTRACT.get(), PIS_PRICE, PIS_ISBN, PIS_PUBLISHED, PIS_READ, PIS_PAGES, 3);
        checkBook(findBookWithTitle(books, EJ2), EJ2_ABSTRACT.get(), EJ2_PRICE, EJ2_ISBN, EJ2_PUBLISHED, EJ2_READ, EJ2_PAGES, 1);
    }

    private void checkBook(Book book, String bookAbstract, double price, long isbn, Date published, boolean read, Integer pages, int i) {
        assertEquals(bookAbstract, book.getAbstract());
        assertEquals(price, book.getPrice(), 0d);
        assertEquals(published.getTime(), book.getPublished().getTime());
        assertEquals(isbn, book.getIsbn());
        assertEquals(read, book.isRead());
        assertEquals(pages, book.getNumberOfPages());
        assertEquals(i, book.getAuthors().length);
    }

    private Book findBookWithTitle(Iterable<Book> books, final String title) {
        return Iterables.find(books, new Predicate<Book>() {
            @Override
            public boolean apply(Book book) {
                return title.equals(book.getTitle());
            }
        });
    }

    private ImmutableList<Book> allBooks() {
        return toList(ao.find(Book.class));
    }

    private <T> ImmutableList<T> toList(T[] authors) {
        return ImmutableList.copyOf(newArrayList(authors));
    }

    private static final class BookAbstractSupplier implements Supplier<String> {
        private final String resource;

        public BookAbstractSupplier(String resource) {
            this.resource = resource;
        }

        @Override
        public String get() {
            return resource == null ? "" : load();
        }

        private String load() {
            InputStream is = null;
            try {
                is = this.getClass().getResourceAsStream(resource);
                return IOUtils.toString(is, "UTF-8");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }

    private static Date toDate(String date) {
        try {
            final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
