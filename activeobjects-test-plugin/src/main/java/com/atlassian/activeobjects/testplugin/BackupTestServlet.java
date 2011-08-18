package com.atlassian.activeobjects.testplugin;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.spi.Backup;
import com.atlassian.activeobjects.spi.NullBackupProgressMonitor;
import com.atlassian.activeobjects.spi.NullRestoreProgressMonitor;
import com.atlassian.activeobjects.spi.OnBackupError;
import com.atlassian.activeobjects.spi.PluginExport;
import com.atlassian.activeobjects.spi.PluginImport;
import com.atlassian.activeobjects.spi.PluginInformation;
import com.atlassian.activeobjects.test.model.Model;
import com.google.common.collect.Lists;

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

public final class BackupTestServlet extends HttpServlet
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

        resp.setContentType("application/xml");
        resp.setCharacterEncoding("UTF-8");

        final OutputStream os = resp.getOutputStream();

        backup.save(new PluginExport()
        {
            @Override
            public OutputStream getOutputStream(PluginInformation info)
            {
                checkState(info.isAvailable());
                checkState(info.getPluginKey().equals("com.atlassian.activeobjects" + "." + "activeobjects-test-plugin"));
                checkState(info.getPluginName().equals("ActiveObjects Plugin - Test Plugin"));

                return os;
            }

            @Override
            public OnBackupError error(PluginInformation information, Throwable t)
            {
                return OnBackupError.FAIL;
            }
        }, NullBackupProgressMonitor.INSTANCE);

        os.flush();
        os.close();
    }

    @Override
    protected void doPost(final HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        backup.restore(Lists.<PluginImport>newArrayList(new PluginImport()
        {
            @Override
            public PluginInformation getPluginInformation()
            {
                return new PluginInformation()
                {
                    @Override
                    public boolean isAvailable()
                    {
                        return false;
                    }

                    @Override
                    public String getPluginName()
                    {
                        return null;
                    }

                    @Override
                    public String getPluginKey()
                    {
                        return null;
                    }

                    @Override
                    public String getPluginVersion()
                    {
                        return null;
                    }

                    @Override
                    public String getHash()
                    {
                        return "0F732C";
                    }
                };
            }

            @Override
            public InputStream getInputStream()
            {
                return newInputStream(req.getParameter(BACKUP));
            }

            @Override
            public OnBackupError error(PluginInformation information, Throwable t)
            {
                return OnBackupError.FAIL;
            }
        }), NullRestoreProgressMonitor.INSTANCE);
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
