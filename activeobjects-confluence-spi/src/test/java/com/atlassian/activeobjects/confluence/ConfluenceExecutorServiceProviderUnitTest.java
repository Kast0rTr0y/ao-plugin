package com.atlassian.activeobjects.confluence;

import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith (MockitoJUnitRunner.class)
public class ConfluenceExecutorServiceProviderUnitTest
{
    private ConfluenceExecutorServiceProvider executorServiceProvider;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private DataSourceProvider dataSourceProvider;

    @Mock
    private ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    @Before
    public void setUp() throws Exception
    {
        executorServiceProvider = new ConfluenceExecutorServiceProvider(threadLocalDelegateExecutorFactory, dataSourceProvider, transactionTemplate);

        when(transactionTemplate.execute(Matchers.any(TransactionCallback.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return ((TransactionCallback<?>) invocation.getArguments()[0]).doInTransaction();
            }
        });
        when(threadLocalDelegateExecutorFactory.createExecutorService(Matchers.any(ExecutorService.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArguments()[0];
            }
        });
    }

    @Test
    public void hsqlSnowflake()
    {
        when(dataSourceProvider.getDatabaseType()).thenReturn(DatabaseType.HSQL);

        assertThat(executorServiceProvider.initExecutorService(), is(ExecutorService.class));
    }

    @Test
    public void realDatabase()
    {
        when(dataSourceProvider.getDatabaseType()).thenReturn(DatabaseType.POSTGRESQL);

        assertThat(executorServiceProvider.initExecutorService(), nullValue());
    }
}
