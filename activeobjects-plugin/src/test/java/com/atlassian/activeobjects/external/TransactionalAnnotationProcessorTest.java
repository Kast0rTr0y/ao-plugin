package com.atlassian.activeobjects.external;

import com.atlassian.activeobjects.tx.Transactional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertSame;

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
