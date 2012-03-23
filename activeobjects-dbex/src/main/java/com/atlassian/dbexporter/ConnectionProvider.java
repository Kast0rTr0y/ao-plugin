package com.atlassian.dbexporter;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * <p>An interface to access a database connection</p>
 * <p>It is left up to implementations to define how the connection is managed (pooled, etc.). However connections should
 * behave consistently when closed, with regards to transactions, etc. I.e. no assumptions should be made on how connections
 * might be used.</p>
 */
public interface ConnectionProvider
{
    Connection getConnection() throws SQLException;
}
