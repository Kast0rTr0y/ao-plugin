package com.atlassian.activeobjects.backup;

import com.atlassian.activeobjects.ao.ActiveObjectsFieldNameConverter;
import com.atlassian.activeobjects.ao.PrefixedSchemaConfiguration;
import com.atlassian.activeobjects.test.model.Model;
import net.java.ao.EntityManager;
import net.java.ao.builder.EntityManagerBuilder;
import net.java.ao.test.jdbc.Hsql;
import net.java.ao.test.jdbc.JdbcConfiguration;
import net.java.ao.test.jdbc.MySql;
import net.java.ao.test.jdbc.Oracle;
import net.java.ao.test.jdbc.Postgres;

import java.io.ByteArrayOutputStream;

public final class CreateBackup
{
    public static void main(String[] args) throws Exception
    {
        final JdbcConfiguration jdbc = new Hsql();
//        final JdbcConfiguration jdbc = new MySql();
//        final JdbcConfiguration jdbc = new Postgres();
//        final JdbcConfiguration jdbc = new Oracle() {
//            public String getUrl()
//    {
//        return "jdbc:oracle:thin:@192.168.0.16:1521:orcl";
//    }
//        };

        final EntityManager entityManager = newEntityManager(jdbc);

        final Model model = new Model(entityManager);
        model.createData();

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new ActiveObjectsBackup(entityManager.getProvider()).save(stream);

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
                .fieldNameConverter(new ActiveObjectsFieldNameConverter())
                .schemaConfiguration(new PrefixedSchemaConfiguration(ActiveObjectsBackup.PREFIX))
                .build();
    }
}
