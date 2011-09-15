package com.atlassian.activeobjects.internal;

import net.java.ao.ActiveObjectsException;
import net.java.ao.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static net.java.ao.sql.SqlUtils.*;

public final class ActiveObjectsSqlException extends ActiveObjectsException
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Database database;
    private Driver driver;

    public ActiveObjectsSqlException(EntityManager entityManager, SQLException cause)
    {
        super(cause);
        getInformation(entityManager);
    }

    public SQLException getSqlException()
    {
        return (SQLException) getCause();
    }

    @Override
    public String getMessage()
    {
        return "There was a SQL exception thrown by the Active Objects library:\n" + database + "\n" + driver + "\n\n" +
                super.getMessage();
    }

    private void getInformation(EntityManager entityManager)
    {
        Connection connection = null;
        try
        {
            connection = entityManager.getProvider().getConnection();
            if (connection != null && !connection.isClosed())
            {
                final DatabaseMetaData metaData = connection.getMetaData();
                database = getDatabase(metaData);
                driver = getDriver(metaData);
            }
        }
        catch (SQLException e)
        {
            logger.debug("Could not load database connection meta data", e);
        }
        finally
        {
            closeQuietly(connection);
        }
    }


    private static Database getDatabase(DatabaseMetaData metaData) throws SQLException
    {
        return new Database(
                getDatabaseName(metaData),
                getDatabaseVersion(metaData),
                getDatabaseMinorVersion(metaData),
                getDatabaseMajorVersion(metaData));
    }

    private static Driver getDriver(DatabaseMetaData metaData) throws SQLException
    {
        return new Driver(
                getDriverName(metaData),
                getDriverVersion(metaData));
    }


    private static String getDatabaseName(DatabaseMetaData metaData) throws SQLException
    {
        return metaData.getDatabaseProductName();
    }

    private static String getDatabaseVersion(DatabaseMetaData metaData) throws SQLException
    {
        return metaData.getDatabaseProductVersion();
    }

    private static String getDatabaseMinorVersion(DatabaseMetaData metaData) throws SQLException
    {
        return String.valueOf(metaData.getDatabaseMinorVersion());
    }

    private static String getDatabaseMajorVersion(DatabaseMetaData metaData) throws SQLException
    {
        return String.valueOf(metaData.getDatabaseMajorVersion());
    }

    private static String getDriverName(DatabaseMetaData metaData) throws SQLException
    {
        return metaData.getDriverName();
    }

    private static String getDriverVersion(DatabaseMetaData metaData) throws SQLException
    {
        return metaData.getDriverVersion();
    }

    private static final class Database
    {
        public final String name;
        public final String version;
        public final String minorVersion;
        public final String majorVersion;

        public Database(String name, String version, String minorVersion, String majorVersion)
        {
            this.name = name;
            this.version = version;
            this.minorVersion = minorVersion;
            this.majorVersion = majorVersion;
        }

        @Override
        public String toString()
        {
            return "Database:\n"
                    + "\t- name:" + name + "\n"
                    + "\t- version:" + version + "\n"
                    + "\t- minor version:" + minorVersion + "\n"
                    + "\t- major version:" + majorVersion;
        }
    }

    private static final class Driver
    {
        public final String name;
        public final String version;

        public Driver(String name, String version)
        {
            this.name = name;
            this.version = version;
        }

        @Override
        public String toString()
        {
            return "Driver:\n"
                    + "\t- name:" + name + "\n"
                    + "\t- version:" + version;
        }
    }
}
