package com.atlassian.activeobjects.confluence.hibernate;

import com.atlassian.hibernate.PluginHibernateSessionFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
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

    private final TransactionTemplate transactionTemplate;

    public HibernateSessionDialectExtractor(PluginHibernateSessionFactory sessionFactory, TransactionTemplate transactionTemplate)
    {
        this.sessionFactory = checkNotNull(sessionFactory);
        this.transactionTemplate = checkNotNull(transactionTemplate);
    }

    public Class<? extends Dialect> getDialect()
    {
        // hibernate needs a transaction to create the session
        return transactionTemplate.execute(new TransactionCallback<Class<? extends Dialect>>()
        {
            @Override
            public Class<? extends Dialect> doInTransaction()
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
        });
    }
}
