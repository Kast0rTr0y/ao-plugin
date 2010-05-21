package com.atlassian.activeobjects.external;

import java.sql.Connection;


public interface TransactionStatus {

    Connection getConnection();
}
