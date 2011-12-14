package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.admin.PluginToTablesMapping;
import com.atlassian.activeobjects.spi.NullBackupProgressMonitor;
import com.atlassian.activeobjects.spi.NullRestoreProgressMonitor;
import com.atlassian.plugin.PluginAccessor;
import net.java.ao.EntityManager;
import net.java.ao.atlassian.AtlassianFieldNameConverter;
import net.java.ao.atlassian.AtlassianIndexNameConverter;
import net.java.ao.atlassian.AtlassianSequenceNameConverter;
import net.java.ao.atlassian.AtlassianTriggerNameConverter;
import net.java.ao.test.converters.NameConverters;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.mockito.Mockito.*;

@RunWith(ActiveObjectsJUnitRunner.class)
@NameConverters(
        table = BackupActiveObjectsTableNameConverter.class,
        field = AtlassianFieldNameConverter.class,
        sequence = AtlassianSequenceNameConverter.class,
        trigger = AtlassianTriggerNameConverter.class,
        index = AtlassianIndexNameConverter.class)
public abstract class AbstractTestActiveObjectsBackup
{
    private static final String UTF_8 = "UTF-8";

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected EntityManager entityManager;
    private ActiveObjectsBackup aoBackup;

    @Before
    public final void setUp()
    {
        aoBackup = new ActiveObjectsBackup(entityManager.getProvider(), entityManager.getNameConverters(), new ImportExportErrorServiceImpl(new PluginInformationFactory(mock(PluginToTablesMapping.class), new ActiveObjectsHashesReader(), mock(PluginAccessor.class))));
    }

    @After
    public final void tearDown()
    {
        aoBackup = null;
    }

    protected final String save()
    {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        aoBackup.save(os, NullBackupProgressMonitor.INSTANCE);
        try
        {
            return os.toString(UTF_8);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected final void restore(String xmlBackup) throws IOException
    {
        aoBackup.restore(IOUtils.toInputStream(xmlBackup, UTF_8), NullRestoreProgressMonitor.INSTANCE);
    }

    protected final String read(String resource) throws IOException
    {
        logger.debug("Reading resource from '{}'", resource);
        InputStream is = null;
        try
        {
            is = this.getClass().getResourceAsStream(resource);
            return IOUtils.toString(is, UTF_8);
        }
        finally
        {
            IOUtils.closeQuietly(is);
        }
    }
}
