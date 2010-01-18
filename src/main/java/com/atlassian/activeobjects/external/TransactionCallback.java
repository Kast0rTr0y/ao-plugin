package com.atlassian.activeobjects.external;

import java.sql.SQLException;

public interface TransactionCallback<T>
{
    T doInTransaction(TransactionStatus transactionStatus) throws SQLException;
}
