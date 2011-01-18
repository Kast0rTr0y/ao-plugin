package com.atlassian.activeobjects.testplugin;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.testplugin.model.Author;
import com.atlassian.activeobjects.testplugin.model.Authorship;
import com.atlassian.activeobjects.testplugin.model.Book;
import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Collections2.*;
import static com.google.common.collect.Lists.newArrayList;

public class BackupTestServlet extends HttpServlet
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String CREATE = "create";
    public static final String BACKUP = "backup";

    private final ActiveObjects ao;
    private final Backup backup;


    public BackupTestServlet(ActiveObjects ao, Backup backup)
    {
        this.ao = checkNotNull(ao);
        this.backup = checkNotNull(backup);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        if (Boolean.valueOf(req.getParameter(CREATE)))
        {
            createData();
        }

        resp.setContentType("application/json");
        final OutputStream os = resp.getOutputStream();

        backup.save(os);

        os.flush();
        os.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        backup.restore(newInputStream(req.getParameter(BACKUP)));
    }

    private InputStream newInputStream(String backupString)
    {
        try
        {
            return new ByteArrayInputStream(backupString.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        emptyDatabase();
    }

    private void createData()
    {
        logger.info("Adding data to the database!");
        resetDatabase();

        final Author[] jcip = authors("Brian Goetz", "Tim Peierls", "Joshua Bloch", "Joseph Bowbeer", "David Holmes", "Doug Lea");
        book("Java Concurrency In Practice", jcip);

        final Author[] scala = authors("Martin Odersky", "Lex Spoon", "Bill Venners");
        book("Programming in Scala", scala);

        book("Effective Java (Second Edition)", jcip[3]); // author is Josh Bloch
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
        final Author author = ao.create(Author.class);
        author.setName(name);
        author.save();
        return author;
    }

    private Book book(String title, Author... authors)
    {
        final Book book = ao.create(Book.class);
        book.setTitle(title);
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
        ao.migrate(Book.class, Author.class, Authorship.class);
    }

    private void emptyDatabase()
    {
        ao.migrate();
    }
}
