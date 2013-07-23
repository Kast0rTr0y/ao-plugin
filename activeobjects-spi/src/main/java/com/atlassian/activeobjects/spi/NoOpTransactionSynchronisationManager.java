package com.atlassian.activeobjects.spi;

/**
 * A simple no-op implementation of the {@link TransactionSynchronisationManager}
 */
public final class NoOpTransactionSynchronisationManager implements TransactionSynchronisationManager
{
    @Override
    public boolean runOnRollBack(Runnable callback)
    {
        return false;
    }

    @Override
    public boolean runOnSuccessfulCommit(Runnable callback)
    {
        return false;
    }

    @Override
    public boolean isActiveSynchronisedTransaction()
    {
        return false;
    }
}
