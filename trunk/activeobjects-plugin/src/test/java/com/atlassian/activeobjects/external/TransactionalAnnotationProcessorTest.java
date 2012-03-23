package com.atlassian.activeobjects.external;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

/**
 * Testing {@link com.atlassian.activeobjects.external.TransactionalAnnotationProcessor}
 */
@RunWith(MockitoJUnitRunner.class)
public class TransactionalAnnotationProcessorTest
{
    private TransactionalAnnotationProcessor transactionalAnnotationProcessor;

    @Mock
    @SuppressWarnings("unused")
    private ActiveObjects ao;

    @Before
    public void setUp() throws Exception
    {
        transactionalAnnotationProcessor = new TransactionalAnnotationProcessor(ao);
    }

    @After
    public void tearDown() throws Exception
    {
        transactionalAnnotationProcessor = null;
    }

    @Test
    public void testPostProcessAfterInitializationDoesNothingWhenNotAnnotated() throws Exception
    {
        final Object o = new Object();
        assertSame(o, transactionalAnnotationProcessor.postProcessAfterInitialization(o, "a-bean-name"));
    }

    @Test
    public void testPostProcessAfterInitializationReturnsProxyWhenAnnotatedAtClassLevel() throws Exception
    {
        final Object o = new AnnotatedInterface()
        {
        };
        final Object proxy = transactionalAnnotationProcessor.postProcessAfterInitialization(o, "a-bean-name");
        assertFalse(o == proxy);
    }

    @Test
    public void testPostProcessAfterInitializationReturnsProxyWhenAnnotatedAtMethodLevel() throws Exception
    {
        final Object o = new AnnotatedMethodInInterface()
        {
            public void doSomething()
            {
            }
        };
        final Object proxy = transactionalAnnotationProcessor.postProcessAfterInitialization(o, "a-bean-name");
        assertFalse(o == proxy);
    }

    @Test
    public void throwingExceptionInTransactionalMethodActuallyThrowsSameException()
    {
        when(ao.executeInTransaction(Matchers.<TransactionCallback<Object>>any())).thenAnswer(new Answer()
        {
            public Object answer(InvocationOnMock invocation)
            {
                return ((TransactionCallback) invocation.getArguments()[0]).doInTransaction();
            }
        });

        final RuntimeException expectedException = new RuntimeException();
        final Object o = new AnnotatedMethodInInterface()
        {
            public void doSomething()
            {
                throw expectedException;
            }
        };

        final AnnotatedMethodInInterface proxy = (AnnotatedMethodInInterface) transactionalAnnotationProcessor.postProcessAfterInitialization(o, "a-bean-name");
        assertFalse(o == proxy);
        try
        {
            proxy.doSomething();
        }
        catch (Exception actualException)
        {
            assertSame(expectedException, actualException);
        }
    }

    @Transactional
    public static interface AnnotatedInterface
    {
    }

    public static interface AnnotatedMethodInInterface
    {
        @Transactional
        @SuppressWarnings("unused")
        public void doSomething();
    }
}
