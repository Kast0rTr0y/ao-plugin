package com.atlassian.activeobjects.confluence.transaction;

import com.atlassian.activeobjects.spi.TransactionSynchronisationManager;
import com.atlassian.confluence.core.SynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

/**
 * An implementation that mostly delegates to confluence core SynchronisationManager.
 *
 * Handles the adaption between Runnables and springs TransactionSynchronization.
 */
public class ConfluenceAOSynchronisationManager implements TransactionSynchronisationManager {
    private SynchronizationManager synchronisationManager;

    public ConfluenceAOSynchronisationManager(SynchronizationManager synchManager) {
        this.synchronisationManager = synchManager;
    }

    @Override
    public boolean runOnRollBack(final Runnable callback) {
        if (synchronisationManager.isTransactionActive()) {
            synchronisationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        callback.run();
                    }
                }
            });
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean runOnSuccessfulCommit(Runnable callback) {
        if (synchronisationManager.isTransactionActive()) {
            synchronisationManager.runOnSuccessfulCommit(callback);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isActiveSynchronisedTransaction() {
        return synchronisationManager.isTransactionActive();
    }
}
