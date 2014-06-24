package com.atlassian.activeobjects.testplugin;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.NullBackupProgressMonitor;
import com.atlassian.activeobjects.spi.NullRestoreProgressMonitor;
import com.atlassian.activeobjects.test.model.Author;
import com.atlassian.activeobjects.test.model.Model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import static com.google.common.base.Preconditions.checkNotNull;

public class BackupTestServlet extends HttpServlet implements InitializingBean
{
    public static final String CREATE = "create";
    public static final String BACKUP = "backup";
    public static final String DELETE = "delete";

    private final ActiveObjects ao;
    private final Backup backup;

    private static Logger log = LoggerFactory.getLogger(BackupTestServlet.class);

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
        else if (Boolean.valueOf(req.getParameter(DELETE)))
        {
            new Model(ao).emptyDatabase();
        }

        resp.setContentType("application/xml");
        resp.setCharacterEncoding("UTF-8");

        final OutputStream os = resp.getOutputStream();

        backup.save(os, NullBackupProgressMonitor.INSTANCE);

        os.flush();
        os.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        backup.restore(newInputStream(req.getParameter(BACKUP)), NullRestoreProgressMonitor.INSTANCE);
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
    
    @Override
    public void afterPropertiesSet() throws Exception
    {
        //accessing active objects during plugin initialization used to blow up : AO-330
        Model model = new Model(ao);
        try
        {
            
            model.migrateEntities();
            model.createData();
        }
        catch(Exception ex)
        {
            throw new RuntimeException("Failed to access active objects during plugin initialization", ex);
        }
        
        // can't use checkXXX methods as junit is not bundled in the plugin
        int authorCount = ao.count(Author.class);
        if (authorCount <= 0)
        {
            throw new IllegalStateException("Should have found some data "+authorCount);
        }
        
        model.emptyDatabase();
        
        log.info("succeeded in accessing active objects whilst initializing a plugin");
    }
}
