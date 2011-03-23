package com.atlassian.activeobjects.web.admin;

import com.atlassian.activeobjects.spi.Backup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static com.google.common.base.Preconditions.*;

/**
 * The controller to backup and restore ActiveObjects plugins
 */
public class BackupRestoreController
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Backup backup;

    public BackupRestoreController(Backup backup)
    {
        this.backup = checkNotNull(backup);
    }

    public ModelAndView backup(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        return new ModelAndView(new AbstractView()
        {
            @Override
            protected boolean generatesDownloadContent()
            {
                return true;
            }

            @Override
            public String getContentType()
            {
                return "application/json";
            }

            @Override
            protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception
            {
                OutputStream os = null;
                try
                {
                    os = response.getOutputStream();
                    backup.save(os);
                }
                finally
                {
                    if (os != null)
                    {
                        os.flush();
                        os.close();
                    }
                }
            }
        });
    }

    public void restore(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        logger.info("Does nothing yet");
    }
}
