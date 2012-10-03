package it.com.atlassian.gadgets.jira.master;

import com.atlassian.jira.nimblefunctests.framework.NimbleFuncTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @since v3.2
 */
public class PluginExistenceTest extends NimbleFuncTestCase {

	public static final String PLUGIN_KEY = "com.atlassian.activeobjects.activeobjects-plugin";

	@Test
	public void testIfPluginIsInstalledAndEnabled() throws Exception
	{
		assertTrue("Plugin aui is not installed", administration.plugins().isPluginInstalled(PLUGIN_KEY));
		assertTrue("Plugin aui is not enabled", administration.plugins().isPluginEnabled(PLUGIN_KEY));
	}

}
