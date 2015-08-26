package com.atlassian.activeobjects.confluence;

import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.activeobjects.spi.InitExecutorServiceProvider;
import com.atlassian.activeobjects.spi.TenantAwareDataSourceProvider;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.tenancy.api.Tenant;
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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfluenceInitExecutorServiceProviderUnitTest {
    private ConfluenceInitExecutorServiceProvider confluenceInitExecutorServiceProvider;

    @Mock
    private InitExecutorServiceProvider defaultInitExecutorServiceProvider;

    @Mock
    private TenantAwareDataSourceProvider tenantAwareDataSourceProvider;

    @Mock
    private ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    @Mock
    private Tenant tenant;

    @Before
    public void setUp() throws Exception {
        confluenceInitExecutorServiceProvider = new ConfluenceInitExecutorServiceProvider(threadLocalDelegateExecutorFactory, tenantAwareDataSourceProvider, defaultInitExecutorServiceProvider);

        when(threadLocalDelegateExecutorFactory.createExecutorService(Matchers.any(ExecutorService.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0];
            }
        });
    }

    @Test
    public void hsqlSnowflake() {
        when(tenantAwareDataSourceProvider.getDatabaseType(tenant)).thenReturn(DatabaseType.HSQL);

        assertThat(confluenceInitExecutorServiceProvider.initExecutorService(tenant), is(ExecutorService.class));
    }

    @Test
    public void realDatabase() {
        when(tenantAwareDataSourceProvider.getDatabaseType(tenant)).thenReturn(DatabaseType.POSTGRESQL);

        final ExecutorService executorService = mock(ExecutorService.class);
        when(defaultInitExecutorServiceProvider.initExecutorService(tenant)).thenReturn(executorService);

        assertThat(confluenceInitExecutorServiceProvider.initExecutorService(tenant), is(executorService));
    }
}
