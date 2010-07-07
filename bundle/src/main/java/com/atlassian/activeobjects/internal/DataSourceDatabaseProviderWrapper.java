package com.atlassian.activeobjects.internal;

import net.java.ao.*;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.schema.ddl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.Set;

public class DataSourceDatabaseProviderWrapper extends DatabaseProvider {

    private static Logger log = LoggerFactory.getLogger( DataSourceDatabaseProviderWrapper.class );

    private DataSource dataSource;
    private DatabaseProvider delegate;

    public DataSourceDatabaseProviderWrapper(DataSource dataSource, DatabaseProvider delegate) {
        super(null, null, null);
        this.dataSource = dataSource;
        this.delegate = delegate;
    }
    
    // Override API

    @Override
    protected Connection getConnectionImpl() throws SQLException {
        // All this ... just for this!!!
        return dataSource.getConnection();
    }

    // Abstract API

    @Override
    public Class<? extends Driver> getDriverClass() throws ClassNotFoundException {
        return delegate.getDriverClass();
    }

    @Override
    protected Set<String> getReservedWords() {
        // Please secure any sharp objects before reading further ...
        try {

            Method method = delegate.getClass().getMethod("getReservedWords");
            method.setAccessible(true);
            return (Set<String>) method.invoke(delegate);

        } catch (InvocationTargetException e) {
            log.error(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            log.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    // Wrapped API

    @Override
    public String[] renderAction(DDLAction action) {
        return delegate.renderAction(action);
    }

    @Override
    public String renderQuery(Query query, TableNameConverter converter, boolean count) {
        return delegate.renderQuery(query, converter, count);
    }

    @Override
    public Object parseValue(int type, String value) {
        return delegate.parseValue(type, value);
    }

    @Override
    public void setQueryStatementProperties(Statement stmt, Query query) throws SQLException {
        delegate.setQueryStatementProperties(stmt, query);
    }

    @Override
    public void setQueryResultSetProperties(ResultSet res, Query query) throws SQLException {
        delegate.setQueryResultSetProperties(res, query);
    }

    @Override
    public ResultSet getTables(Connection conn) throws SQLException {
        return delegate.getTables(conn);
    }

    @Override
    public String getURI() {
        return delegate.getURI();
    }

    @Override
    public String getUsername() {
        return delegate.getUsername();
    }

    @Override
    public String getPassword() {
        return delegate.getPassword();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public <T> T insertReturningKey(EntityManager manager, Connection conn, Class<T> pkType, String pkField, boolean pkIdentity, String table, DBParam... params) throws SQLException {
        return delegate.insertReturningKey(manager, conn, pkType, pkField, pkIdentity, table, params);
    }

    @Override
    public void putNull(PreparedStatement stmt, int index) throws SQLException {
        delegate.putNull(stmt, index);
    }

    @Override
    public void putBoolean(PreparedStatement stmt, int index, boolean value) throws SQLException {
        delegate.putBoolean(stmt, index, value);
    }

    @Override
    public String processID(String id) {
        return delegate.processID(id);
    }

    @Override
    public boolean isCaseSensetive() {
        return delegate.isCaseSensetive();
    }

}
