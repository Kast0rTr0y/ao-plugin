package com.atlassian.labs.activeobjects.external;

import java.sql.SQLException;

public interface TransactionCallback<T>
{
    T doInTransaction() throws SQLException;
}
