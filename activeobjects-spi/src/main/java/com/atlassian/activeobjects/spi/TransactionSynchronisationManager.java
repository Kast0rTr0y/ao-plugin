package com.atlassian.activeobjects.spi;

public interface TransactionSynchronisationManager {
    /**
     * Add an action to run if the encompassing transaction is rolled back
     *
     * @param callback - the action to run
     * @return true if it has been added to an encompassing transaction,
     * false if there was no transaction to synchronise with
     */
    public boolean runOnRollBack(Runnable callback);

    /**
     * Add an action to run if the encompassing transaction is successfully committed
     *
     * @param callback - the action to run
     * @return true if it has been added to an encompassing transaction,
     * false if there was no transaction to synchronise with
     */
    public boolean runOnSuccessfulCommit(Runnable callback);

    /**
     * @return true if there is a current active transaction
     * @since 0.24
     */
    public boolean isActiveSynchronisedTransaction();
}
