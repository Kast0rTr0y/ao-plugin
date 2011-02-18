package com.atlassian.activeobjects.confluence.hibernate;

import com.atlassian.hibernate.PluginHibernateSessionFactory;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.dialect.Dialect;
import net.sf.hibernate.engine.SessionFactoryImplementor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Extracts the dialect from the {@link net.sf.hibernate.SessionFactory hibernate session factory}, provided the
 * concrete implementation, also implements {@link net.sf.hibernate.engine.SessionFactoryImplementor}.
 */
public final class HibernateSessionDialectExtractor implements DialectExtractor
{
    private final PluginHibernateSessionFactory sessionFactory;

    public HibernateSessionDialectExtractor(PluginHibernateSessionFactory sessionFactory)
    {
        this.sessionFactory = checkNotNull(sessionFactory);
    }

    public Class<? extends Dialect> getDialect()
    {
        final SessionFactory hibernateSessionFactory = sessionFactory.getSession().getSessionFactory();
        if (hibernateSessionFactory instanceof SessionFactoryImplementor)
        {
            return ((SessionFactoryImplementor) hibernateSessionFactory).getDialect().getClass();
        }
        else
        {
            return null;
        }
    }
}