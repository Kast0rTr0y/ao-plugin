package com.atlassian.activeobjects.confluence.backup;

import com.atlassian.activeobjects.spi.RestoreProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of the backup progress monitor that logs progress to SLF4J.
 *
 * Most logging is performed at INFO level.
 */
public class LoggingRestoreProgressMonitor implements RestoreProgressMonitor {
    private static final Logger log = LoggerFactory.getLogger(LoggingRestoreProgressMonitor.class);

    @Override
    public void beginRestore() {
        log.warn("Begin restoring Active Objects backup, adjust log level for com.atlassian.activeobjects.confluence.backup for more detailed logging.");
    }

    @Override
    public void endRestore() {
        log.warn("Completed restoring Active Objects Backup.");

    }

    @Override
    public void beginDatabaseInformationRestore() {
        log.info("Begin restoring database information");
    }

    @Override
    public void beginTableDefinitionsRestore() {
        log.info("Begin restoring table definitions");
    }

    @Override
    public void beginTablesRestore() {
        log.info("Begin restoring tables");
    }

    @Override
    public void beginTableDataRestore(String tableName) {
        log.info("Begin restoring table data for : {}", tableName);
    }

    @Override
    public void beginTableCreationRestore(String tableName) {
        log.info("Begin table creation for : {}", tableName);
    }

    @Override
    public void beginTableRowRestore() {
    }

    @Override
    public void endDatabaseInformationRestore() {
        log.info("Completed database information restore");
    }

    @Override
    public void endTableDefinitionsRestore() {
        log.info("Completed table definitions restore");
    }

    @Override
    public void endTablesRestore() {
        log.info("Completed restoring tables");
    }

    @Override
    public void endTableDataRestore(String tableName) {
        log.info("Completed table data restore for : {}", tableName);
    }

    @Override
    public void endTableCreationRestore(String tableName) {
        log.info("Completed table creation for : {}", tableName);
    }

    @Override
    public void endTableRowRestore() {

    }

    @Override
    public void updateTotalNumberOfTablesToRestore(int tableCount) {
        log.info("Update total number of tables to restore to : {}", tableCount);
    }
}
