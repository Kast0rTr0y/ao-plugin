package it.com.atlassian.activeobjects;

import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.test.ActiveObjectsPluginFile;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.activeobjects.junit.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

/**
 * <p>This configures the basics of the system on which the Active Objects plugin can run.</p>
 * <p>This is essentially the host components and exported packages.</p>
 */
@RunWith(AtlassianPluginsJUnitRunner.class)
@com.atlassian.activeobjects.junit.Plugins(ActiveObjectsPluginFile.class)
@Host(
        version = "1.0",
        includes = {
                "com.atlassian.activeobjects.spi",
                "com.atlassian.plugin*",
                "com_cenqua_clover",
                "com.google.common*",
                "it.com.atlassian.activeobjects",
                "javax.servlet*",
                "org.dom4j*",
                "org.hsqldb"
        },
        excludes = {
                "com.atlassian.activeobjects",
                "com.atlassian.activeobjects.ao*",
                "com.atlassian.activeobjects.admin*",
                "com.atlassian.activeobjects.backup*",
                "com.atlassian.activeobjects.config*",
                "com.atlassian.activeobjects.external*",
                "com.atlassian.activeobjects.internal*",
                "com.atlassian.activeobjects.osgi*",
                "com.atlassian.activeobjects.plugin*",
                "com.atlassian.activeobjects.spring*",
                "com.atlassian.activeobjects.test*",
                "com.atlassian.activeobjects.tx*",
                "com.atlassian.activeobjects.util*"
        },
        versions = {
                @PackageVersion(value = "com.atlassian.activeobjects*", version = "100")
        }
)
public abstract class BaseActiveObjectsIntegrationTest
{
    /**
     * Using a 'configurable' {@link org.junit.rules.TemporaryFolder} so that we can keep the temporary folder for inspection when needed.
     *
     * @see com.atlassian.activeobjects.junit.ConfigurableTemporaryFolder#ConfigurableTemporaryFolder(boolean)
     */
    @Rule
    public TemporaryFolder folder = new ConfigurableTemporaryFolder();

    protected AtlassianPluginsContainer container;

    @MockHostComponent
    private TransactionTemplate transactionTemplate;

    @MockHostComponent
    private PluginAccessor pluginAccessor;

    @MockHostComponent
    protected ApplicationProperties applicationProperties;

    @MockHostComponent
    protected PluginSettingsFactory pluginSettingsFactory;

    @MockHostComponent
    protected DataSourceProvider dataSourceProvider;
}
