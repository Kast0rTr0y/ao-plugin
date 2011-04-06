package com.atlassian.activeobjects.ao;

import com.atlassian.activeobjects.internal.Prefix;
import net.java.ao.RawEntity;
import net.java.ao.schema.Case;
import net.java.ao.schema.TableAnnotationTableNameConverter;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.schema.UnderscoreTableNameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;

/**
 * <p>This is the table name converter used by the Active Objects plugin. It works according to the following:
 * <ul>
 * <li>If the {@link RawEntity entity interface} is annotated with {@link net.java.ao.schema.Table} then the value
 * of the annotation is used as the base for naming.</li>
 * <li>Otherwise the {@link Class#getSimpleName() simple class name} is used.
 * </ul>
 * Then the following transformations are applied, in order:
 * <ol>
 * <li>The base name is transform from camel case to under score and upper case. e.q. {@code MyEntity} becomes
 * {@code MY_ENTITY}.</li>
 * <li>The {@link Prefix prefix} is then applied, using its {@link Prefix#prepend(String) prepend} method.</li>
 * </ol>
 * </p>
 * <p>This means that if you refactor your entities and don't want to have to upgrade the database tables used behind
 * the scene, you can use the {@link net.java.ao.schema.Table} annotation to your advantage. For example, one could
 * refactor the following entity:
 * <pre>
 *     public interface MyEntity {}
 * </pre>
 * to
 * <pre>
 *     &#64;Table("MyEntity")
 *     public interface YourEntity {}
 * </pre>
 */
public final class ActiveObjectsTableNameConverter implements TableNameConverter
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TableNameConverter tableNameConverter;

    public ActiveObjectsTableNameConverter(Prefix prefix)
    {
        // this the basic conversion we want, under score and upper case
        final UnderscoreTableNameConverter baseConverter = new UnderscoreTableNameConverter(Case.UPPER);

        tableNameConverter =
                new PrefixedTableNameConverter(
                        checkNotNull(prefix),
                        new TableAnnotationTableNameConverter(baseConverter, baseConverter));
    }

    @Override
    public String getName(Class<? extends RawEntity<?>> entityClass)
    {
        final String name = tableNameConverter.getName(entityClass);
        logger.debug("Table name for '{}' is '{}'", entityClass.getName(), name);
        return name;
    }
}
