package com.atlassian.activeobjects.confluence.backup;

import com.atlassian.activeobjects.spi.BackupProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of the backup progress monitor that logs progress to SLF4J.
 *
 * Most logging is performed at INFO level.
 */
public class LoggingBackupProgressMonitor implements BackupProgressMonitor {
    private static final Logger log = LoggerFactory.getLogger(LoggingBackupProgressMonitor.class);

    @Override
    public void beginBackup() {
        log.warn("Begin Active objects backup, change log level to INFO for com.atlassian.activeobjects.confluence.backup for more detailed logging.");
    }

    @Override
    public void endBackup() {
        log.warn("Completed active objects backup.");
    }

    @Override
    public void beginDatabaseInformationBackup() {
        log.info("Begin database information backup");
    }

    @Override
    public void beginTableDefinitionsBackup() {
        log.info("Begin table definition backup");
    }

    @Override
    public void beginTablesBackup() {
        log.info("Begin tables backup");
    }

    @Override
    public void beginTableBackup(String tableName) {
        log.info("Begin backup for table : {}", tableName);

    }

    @Override
    public void updateTotalNumberOfTablesToBackup(int tableCount) {
        log.info("update total number of tables to backup to : " + tableCount);
    }

    @Override
    public void endDatabaseInformationBackup() {
        log.info("end database information backup");
    }

    @Override
    public void endTableDefinitionsBackup() {
        log.info("end table definitions backup");
    }

    @Override
    public void endTablesBackup() {
        log.info("finished tables backup");
    }

    @Override
    public void endTableBackup(String tableName) {
        log.info("finished backing up table : {}", tableName);
    }
}
