package com.atlassian.activeobjects.confluence.hibernate;

import com.atlassian.hibernate.PluginHibernateSessionFactory;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.dialect.Dialect;
import net.sf.hibernate.engine.SessionFactoryImplementor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testing {@link com.atlassian.activeobjects.confluence.hibernate.HibernateSessionDialectExtractor}
 */
@RunWith(MockitoJUnitRunner.class)
public class HibernateSessionDialectExtractorTest
{
    private HibernateSessionDialectExtractor dialectExtractor;

    @Mock
    private PluginHibernateSessionFactory pluginSessionFactory;

    @Before
    public void setUp() throws Exception
    {
        dialectExtractor = new HibernateSessionDialectExtractor(pluginSessionFactory);
    }


    @After
    public void tearDown() throws Exception
    {
        dialectExtractor = null;
    }

    @Test
    public void testGetDialectReturnsNullIfSessionFactoryDoesNotImplementSessionFactoryImplementor() throws Exception
    {
        mockPluginSessionFactory(SessionFactory.class);
        assertNull(dialectExtractor.getDialect());
    }

    @Test
    public void testGetDialectReturnsCorrectDialectIfSessionFactoryDoesImplementSessionFactoryImplementor() throws Exception
    {
        final Dialect dialect = mock(Dialect.class);
        final SessionFactoryImplementor sessionFactory = mockPluginSessionFactory(SessionFactoryImplementor.class);
        when(sessionFactory.getDialect()).thenReturn(dialect);

        assertEquals(dialect.getClass(), dialectExtractor.getDialect());
    }

    private <S extends SessionFactory> S mockPluginSessionFactory(Class<S> sessionFactoryClass)
    {
        final Session session = mock(Session.class);
        final S sessionFactory = mock(sessionFactoryClass);
        when(pluginSessionFactory.getSession()).thenReturn(session);
        when(session.getSessionFactory()).thenReturn(sessionFactory);
        return sessionFactory;
    }
}
