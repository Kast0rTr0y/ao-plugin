package com.atlassian.dbexporter;

import java.util.Locale;

/**
 * A class that gives easy access to some database information
 * @author Samuel Le Berrigaud
 */
public final class DatabaseInformations
{
    private DatabaseInformations()
    {
    }

    public static Database database(DatabaseInformation info)
    {
        return new DatabaseImpl(info.get("database.name", new DatabaseTypeConverter()));
    }

    public static interface Database
    {
        Type getType();

        static enum Type
        {
            HSQL,
            MYSQL,
            POSTGRES,
            ORACLE,
            MSSQL,
            UNKNOWN
        }
    }

    private static class DatabaseTypeConverter extends DatabaseInformation.AbstractStringConverter<Database.Type>
    {
        @Override
        public Database.Type convert(String dbName)
        {
            if (isEmpty(dbName))
            {
                return Database.Type.UNKNOWN;
            }

            if (isHsql(dbName))
            {
                return Database.Type.HSQL;
            }

            if (isMySql(dbName))
            {
                return Database.Type.MYSQL;
            }

            if (isPostgres(dbName))
            {
                return Database.Type.POSTGRES;
            }

            if (isOracle(dbName))
            {
                return Database.Type.ORACLE;
            }

            if (isMsSql(dbName))
            {
                return Database.Type.MSSQL;
            }

            return Database.Type.UNKNOWN;
        }

        private boolean isEmpty(String dbName)
        {
            return dbName == null || dbName.trim().length() == 0;
        }

        private boolean isHsql(String dbName)
        {
            return startsWithIgnoreCase(dbName, "HSQL");
        }

        private boolean isMySql(String dbName)
        {
            return startsWithIgnoreCase(dbName, "MySQL");
        }

        private boolean isPostgres(String dbName)
        {
            return startsWithIgnoreCase(dbName, "PostgreSQL");
        }

        private boolean isOracle(String dbName)
        {
            return startsWithIgnoreCase(dbName, "Oracle");
        }

        private boolean isMsSql(String dbName)
        {
            return startsWithIgnoreCase(dbName, "Microsoft");
        }

        private boolean startsWithIgnoreCase(String s, String start)
        {
            return toLowerCase(s).startsWith(toLowerCase(start));
        }

        private String toLowerCase(String s)
        {
            return s == null ? s : s.toLowerCase(Locale.ENGLISH);
        }
    }

    private static class DatabaseImpl implements Database
    {
        private final Type type;

        public DatabaseImpl(Type type)
        {
            this.type = type;
        }

        @Override
        public Type getType()
        {
            return type;
        }

        @Override
        public String toString()
        {
            return new StringBuilder().append(getType()).toString();
        }
    }
}
