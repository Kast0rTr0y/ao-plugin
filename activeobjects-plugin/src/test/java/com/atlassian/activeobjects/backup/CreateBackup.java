package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.admin.PluginToTablesMapping;
import com.atlassian.activeobjects.ao.PrefixedSchemaConfiguration;
import com.atlassian.activeobjects.spi.NullBackupProgressMonitor;
import com.atlassian.activeobjects.test.model.Model;
import com.atlassian.dbexporter.ImportExportErrorService;
import com.atlassian.plugin.PluginAccessor;
import com.google.common.collect.ImmutableMap;
import net.java.ao.EntityManager;
import net.java.ao.atlassian.AtlassianFieldNameConverter;
import net.java.ao.builder.EntityManagerBuilder;
import net.java.ao.test.jdbc.Hsql;
import net.java.ao.test.jdbc.JdbcConfiguration;
import net.java.ao.test.jdbc.MySql;
import net.java.ao.test.jdbc.Oracle;
import net.java.ao.test.jdbc.Postgres;
import net.java.ao.test.jdbc.SqlServer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import static org.mockito.Mockito.*;

public final class CreateBackup
{
    private final static ImmutableMap<String, JdbcConfiguration> JDBC = ImmutableMap.<String, JdbcConfiguration>builder()
            .put("hsql", new Hsql())
            .put("mysql", new MySql())
            .put("postgres", new Postgres())
            .put("oracle", new Oracle())
            .put("sqlserver", new SqlServer())
            .build();

    private static final String CUSTOM = "custom";

    public static void main(String[] args) throws Exception
    {
        final ImportExportErrorService errorService = new ImportExportErrorServiceImpl(new PluginInformationFactory(mock(PluginToTablesMapping.class), new ActiveObjectsHashesReader(), mock(PluginAccessor.class)));

        final JdbcConfiguration jdbc = selectJdbcDriver();

        final EntityManager entityManager = newEntityManager(jdbc);

        final Model model = new Model(entityManager);
        model.createData();

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new ActiveObjectsBackup(entityManager.getProvider(), entityManager.getNameConverters(), errorService).save(stream, NullBackupProgressMonitor.INSTANCE);

        System.out.println(stream.toString("UTF-8"));
    }

    private static EntityManager newEntityManager(JdbcConfiguration jdbc)
    {
        return EntityManagerBuilder
                .url(jdbc.getUrl())
                .username(jdbc.getUsername())
                .password(jdbc.getPassword())
                .auto()
                .tableNameConverter(new BackupActiveObjectsTableNameConverter())
                .fieldNameConverter(new AtlassianFieldNameConverter())
                .schemaConfiguration(new PrefixedSchemaConfiguration(ActiveObjectsBackup.PREFIX))
                .build();
    }

    private static JdbcConfiguration selectJdbcDriver() throws IOException
    {
        final String choice = chooseJdbcConfiguration();
        if (CUSTOM.equalsIgnoreCase(choice))
        {
            return customJdbc();
        }
        else if (JDBC.containsKey(choice))
        {
            return JDBC.get(choice);
        }
        else
        {
            System.out.println("Please choose a valid JDBC configuration!");
            return selectJdbcDriver();
        }
    }

    private static JdbcConfiguration customJdbc() throws IOException
    {
        final String url = prompt("Url:");
        final String username = prompt("Username:", "ao_user");
        final String password = prompt("Password:", "ao_password");
        final String schema = prompt("Schema:", "");

        return new JdbcConfiguration()
        {
            @Override
            public String getUrl()
            {
                return url;
            }

            @Override
            public String getSchema()
            {
                return schema;
            }

            @Override
            public String getUsername()
            {
                return username;
            }

            @Override
            public String getPassword()
            {
                return password;
            }
        };
    }

    private static String chooseJdbcConfiguration() throws IOException
    {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, JdbcConfiguration> configs : JDBC.entrySet())
        {
            final JdbcConfiguration jdbc = configs.getValue();
            sb.append(configs.getKey()).append(": ")
                    .append(jdbc.getUrl()).append(" - <")
                    .append(jdbc.getSchema()).append("> - ")
                    .append(jdbc.getUsername()).append(" - ")
                    .append(jdbc.getPassword()).append("\n");
        }
        sb.append(CUSTOM).append("\n");

        return prompt("Choose a configuration:\n" + sb, JDBC.keySet().iterator().next());
    }

    private static String prompt(String message) throws IOException
    {
        return prompt(message, null);
    }

    private static String prompt(String message, String defaultValue) throws IOException
    {
        System.out.println(isNotEmpty(defaultValue) ? message + "[" + defaultValue + "]" : message);
        final String value = new BufferedReader(new InputStreamReader(System.in)).readLine();
        return isNotEmpty(value) ? value : defaultValue;
    }

    private static boolean isNotEmpty(String s)
    {
        return s != null && !s.equals("");
    }
}
