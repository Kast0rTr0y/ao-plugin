package com.atlassian.activeobjects.refapp;

import com.atlassian.sal.api.ApplicationProperties;
import org.hsqldb.jdbc.jdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 */
public final class RefappDataSource implements DataSource {
    private static final String DEFAULT_BASE_DIR = "data/plugins/activeobjects";
    private static final String DEFAULT_USERNAME = "sa";
    private static final String DEFAULT_PASSWORD = "";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final DataSource delegate;

    public RefappDataSource(ApplicationProperties applicationProperties) {
        checkNotNull(applicationProperties);
        delegate = createDelegate(checkHomeDirectory(applicationProperties.getHomeDirectory()));
    }

    private DataSource createDelegate(final File homeDirectory) {
        final jdbcDataSource dataSource = new jdbcDataSource();
        dataSource.setDatabase(createJdbcUrl(homeDirectory));
        dataSource.setUser(DEFAULT_USERNAME);
        dataSource.setPassword(DEFAULT_PASSWORD);
        return dataSource;
    }

    private String createJdbcUrl(final File homeDirectory) {
        final File dbDirectory = new File(homeDirectory, DEFAULT_BASE_DIR);
        if (dbDirectory.exists() && dbDirectory.isFile()) {
            throw new RuntimeException("Database directory already exists, but is a file, at <" + dbDirectory.getPath() + ">");
        }

        if (!dbDirectory.exists() && !dbDirectory.mkdirs()) {
            throw new RuntimeException("Could not create directory for database at <" + dbDirectory.getPath() + ">");
        }

        log.debug("ActiveObjects databases directory {} initialized", dbDirectory.getAbsolutePath());

        return new StringBuilder("jdbc:hsqldb:")
                .append(dbDirectory.getAbsolutePath())
                .append(";hsqldb.default_table_type=cached").toString();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return delegate.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return delegate.getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> tClass) throws SQLException {
        return delegate.unwrap(tClass);
    }

    @Override
    public boolean isWrapperFor(Class<?> aClass) throws SQLException {
        return delegate.isWrapperFor(aClass);
    }

    // @Override Java 7 only
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    private static File checkHomeDirectory(final File homeDirectory) {
        if (homeDirectory == null) {
            throw new RuntimeException("no home directory defined by product");
        }

        if (!homeDirectory.exists() || !homeDirectory.isDirectory()) {
            throw new RuntimeException("Couldn't find product home directory at '" + homeDirectory.getAbsolutePath() + "'");
        }
        return homeDirectory;
    }
}
