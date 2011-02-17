package com.atlassian.activeobjects.testplugin;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.test.model.Model;

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
import static com.google.common.collect.Lists.newArrayList;

public class BackupTestServlet extends HttpServlet
{
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
            new Model(ao).createData();
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
        new Model(ao).emptyDatabase();
    }
}
