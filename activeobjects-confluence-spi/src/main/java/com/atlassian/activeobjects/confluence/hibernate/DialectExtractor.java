package com.atlassian.activeobjects.confluence.hibernate;

import net.sf.hibernate.dialect.Dialect;

/**
 * Allows to find the current Hibernate dialect
 */
public interface DialectExtractor {
    /**
     * Gets the dialect currently used.
     *
     * @return the currently used dialect. {@code null} if the information could not be resolved/extracted.
     */
    Class<? extends Dialect> getDialect();
}
