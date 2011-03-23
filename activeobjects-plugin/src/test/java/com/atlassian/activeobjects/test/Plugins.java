package com.atlassian.activeobjects.test;

import com.atlassian.plugin.test.PluginJarBuilder;

import java.io.File;

/** Some useful plugins to use for testing */
public final class Plugins
{
    /**
     * A simple plugin that is a basic consumer of the Active Objects service
     *
     * @param pluginKey the key to use for the plugin
     * @return a ready-built plugin file
     * @throws Exception any error that might happen compiling, building the plugin
     */
    public static File newConsumerPlugin(String pluginKey) throws Exception
    {
        return new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public interface Foo extends net.java.ao.Entity {",
                        " public String getName();",
                        " public void setName(String name);",
                        "}")
                .addFormattedJava("my.FooComponent",
                        "package my;",
                        "import com.atlassian.activeobjects.external.*;",
                        "public class FooComponent implements it.com.atlassian.activeobjects.ActiveObjectsTestConsumer, com.atlassian.sal.api.transaction.TransactionCallback {",
                        "  ActiveObjects mgr;",
                        "  public FooComponent(ActiveObjects mgr) throws Exception {",
                        "    this.mgr = mgr;",
                        "  }",
                        "  public Object run() throws Exception {",
                        "    return mgr.executeInTransaction(this);",
                        "  }",
                        "  public Object doInTransaction() {",
                        "    try {",
                        "        Foo foo = (Foo) mgr.create(my.Foo.class, new net.java.ao.DBParam[0]);",
                        "        foo.setName('bob');",
                        "        foo.save();",
                        "        foo = (Foo) mgr.find(Foo.class, 'id = ?', new Object[]{foo.getID()})[0];",
                        "        if (foo == null) throw new RuntimeException('no foo found');",
                        "        if (foo.getName() == null) throw new RuntimeException('foo has no name');",
                        "        if (!foo.getName().equals('bob')) throw new RuntimeException('foo name wrong');",
                        "        return null;",
                        "    } catch (java.sql.SQLException e) { throw new RuntimeException(e); }",
                        "  }",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='" + pluginKey + "' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='obj' class='my.FooComponent' public='true' interface='it.com.atlassian.activeobjects.ActiveObjectsTestConsumer' />",
                        "    <ao key='ao'>",
                        "        <entity>my.Foo</entity>",
                        "    </ao>",
                        "    <component-import key='emp' interface='com.atlassian.activeobjects.external.ActiveObjects' />",
                        "</atlassian-plugin>")
                .build();
    }

    /**
     * A configuration plugin used to configure some aspect of the Active Objects plugin.
     *
     * @param pluginKey the key to use for that plugin
     * @param databaseDirectoryPath the database directory path to use
     * @return a ready-built plugin file
     * @throws Exception any error that might happen compiling, building the plugin
     */
    public static File newConfigurationPlugin(String pluginKey, String databaseDirectoryPath) throws Exception
    {
        return new PluginJarBuilder()
                .addFormattedJava("config.MyConfig",
                        "package config;",
                        "public class MyConfig implements com.atlassian.activeobjects.spi.ActiveObjectsPluginConfiguration {",
                        "  public String getDatabaseBaseDirectory() { return '" + databaseDirectoryPath + "'; }",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='" + pluginKey + "' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='obj' class='config.MyConfig' public='true' interface='com.atlassian.activeobjects.spi.ActiveObjectsPluginConfiguration' />",
                        "</atlassian-plugin>")
                .build();
    }
}
