package com.atlassian.activeobjects.testplugin;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.testplugin.model.Book;
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
        backup.restore(backup.getId(), newInputStream(req.getParameter(BACKUP)));
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
        addBook("Java Concurrency In Practice");
    }

    private void addBook(String title)
    {
        final Book book = ao.create(Book.class);
        book.setTitle(title);
        book.save();
    }

    private void resetDatabase()
    {
        emptyDatabase();
        ao.migrate(Book.class);
    }

    private void emptyDatabase()
    {
        ao.migrate();
    }
}
