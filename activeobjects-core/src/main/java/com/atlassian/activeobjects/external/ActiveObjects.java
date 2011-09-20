package com.atlassian.activeobjects.external;

import java.util.Map;

import net.java.ao.Accessor;
import net.java.ao.DBParam;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * Interface to the active objects framework.  Instance is threadsafe.
 */
public interface ActiveObjects
{
    /**
     * Creates the schema for the specified entities
     */
    public void migrate(Class<? extends RawEntity<?>>... entities);

    /**
     * Flushes all value caches contained within entities controlled by this <code>EntityManager</code>
     * instance.  This does not actually remove the entities from the instance cache maintained
     * within this class.  Rather, it simply dumps all of the field values cached within the entities
     * themselves (with the exception of the primary key value).  This should be used in the case
     * of a complex process outside AO control which may have changed values in the database.  If
     * it is at all possible to determine precisely which rows have been changed, the {@link #flush(net.java.ao.RawEntity...)}
     * method should be used instead.
     */
    public void flushAll();

    /**
     * Flushes the value caches of the specified entities along with all of the relevant
     * relations cache entries.  This should be called after a process outside of AO control
     * may have modified the values in the specified rows.  This does not actually remove
     * the entity instances themselves from the instance cache.  Rather, it just flushes all
     * of their internally cached values (with the exception of the primary key).
     */
    public void flush(RawEntity<?>... entities);

    /**
     * <p>Returns an array of entities of the specified type corresponding to the
     * varargs primary keys.  If an in-memory reference already exists to a corresponding
     * entity (of the specified type and key), it is returned rather than creating
     * a new instance.</p>
     * <p/>
     * <p>No checks are performed to ensure that the key actually exists in the
     * database for the specified object.  Thus, this method is solely a Java
     * memory state modifying method.  There is no database access involved.
     * The upshot of this is that the method is very very fast.  The flip side of
     * course is that one could conceivably maintain entities which reference
     * non-existant database rows.</p>
     *
     * @param type The type of the entities to retrieve.
     * @param keys The primary keys corresponding to the entities to retrieve.  All
     * keys must be typed according to the generic type parameter of the entity's
     * {@link RawEntity} inheritence (if inheriting from {@link net.java.ao.Entity}, this is <code>Integer</code>
     * or <code>int</code>).  Thus, the <code>keys</code> array is type-checked at compile
     * time.
     * @return An array of entities of the given type corresponding with the specified primary keys.
     */
    public <T extends RawEntity<K>, K> T[] get(Class<T> type, K... keys);

    /**
     * Cleverly overloaded method to return a single entity of the specified type
     * rather than an array in the case where only one ID is passed.  This method
     * meerly delegates the call to the overloaded <code>get</code> method
     * and functions as syntactical sugar.
     *
     * @param type The type of the entity instance to retrieve.
     * @param key The primary key corresponding to the entity to be retrieved.
     * @return An entity instance of the given type corresponding to the specified primary key.
     * @see #get(Class, Object...)
     */
    public <T extends RawEntity<K>, K> T get(Class<T> type, K key);

    /**
     * <p>Creates a new entity of the specified type with the optionally specified
     * initial parameters.  This method actually inserts a row into the table represented
     * by the entity type and returns the entity instance which corresponds to that
     * row.</p>
     * <p/>
     * <p>The {@link net.java.ao.DBParam} object parameters are designed to allow the creation
     * of entities which have non-null fields which have no defalut or auto-generated
     * value.  Insertion of a row without such field values would of course fail,
     * thus the need for db params.  The db params can also be used to set
     * the values for any field in the row, leading to more compact code under
     * certain circumstances.</p>
     * <p/>
     * <p>Unless within a transaction, this method will commit to the database
     * immediately and exactly once per call.  Thus, care should be taken in
     * the creation of large numbers of entities.  There doesn't seem to be a more
     * efficient way to create large numbers of entities, however one should still
     * be aware of the performance implications.</p>
     * <p/>
     * <p>This method delegates the action INSERT action to
     * This is necessary because not all databases support the JDBC <code>RETURN_GENERATED_KEYS</code>
     * constant (e.g. PostgreSQL and HSQLDB).  Thus, the database provider itself is
     * responsible for handling INSERTion and retrieval of the correct primary key
     * value.</p>
     *
     * @param type The type of the entity to INSERT.
     * @param params An optional varargs array of initial values for the fields in the row.  These
     * values will be passed to the database within the INSERT statement.
     * @return The new entity instance corresponding to the INSERTed row.
     * @see net.java.ao.DBParam
     */
    public <T extends RawEntity<K>, K> T create(Class<T> type, DBParam... params);

    /**
     * Creates and INSERTs a new entity of the specified type with the given map of
     * parameters.  This method merely delegates to the {@link #create(Class, DBParam...)}
     * method.  The idea behind having a separate convenience method taking a map is in
     * circumstances with large numbers of parameters or for people familiar with the
     * anonymous inner class constructor syntax who might be more comfortable with
     * creating a map than with passing a number of objects.
     *
     * @param type The type of the entity to INSERT.
     * @param params A map of parameters to pass to the INSERT.
     * @return The new entity instance corresponding to the INSERTed row.
     * @see #create(Class, DBParam...)
     */
    public <T extends RawEntity<K>, K> T create(Class<T> type, Map<String, Object> params);

    /**
     * <p>Deletes the specified entities from the database.  DELETE statements are
     * called on the rows in the corresponding tables and the entities are removed
     * from the instance cache.  The entity instances themselves are not invalidated,
     * but it doesn't even make sense to continue using the instance without a row
     * with which it is paired.</p>
     * <p/>
     * <p>This method does attempt to group the DELETE statements on a per-type
     * basis.  Thus, if you pass 5 instances of <code>EntityA</code> and two
     * instances of <code>EntityB</code>, the following SQL prepared statements
     * will be invoked:</p>
     * <p/>
     * <pre>DELETE FROM entityA WHERE id IN (?,?,?,?,?);
     * DELETE FROM entityB WHERE id IN (?,?);</pre>
     * <p/>
     * <p>Thus, this method scales very well for large numbers of entities grouped
     * into types.  However, the execution time increases linearly for each entity of
     * unique type.</p>
     *
     * @param entities A varargs array of entities to delete.  Method returns immediately
     * if length == 0.
     */
    @SuppressWarnings("unchecked")
    public void delete(RawEntity<?>... entities);

    /**
     * Returns all entities of the given type.  This actually peers the call to
     * the {@link #find(Class, net.java.ao.Query)} method.
     *
     * @param type The type of entity to retrieve.
     * @return An array of all entities which correspond to the given type.
     */
    public <T extends RawEntity<K>, K> T[] find(Class<T> type);

    /**
     * <p>Convenience method to select all entities of the given type with the
     * specified, parameterized criteria.  The <code>criteria</code> String
     * specified is appended to the SQL prepared statement immediately
     * following the <code>WHERE</code>.</p>
     * <p/>
     * <p>Example:</p>
     * <p/>
     * <pre>manager.find(Person.class, "name LIKE ? OR age &gt; ?", "Joe", 9);</pre>
     * <p/>
     * <p>This actually delegates the call to the {@link #find(Class, net.java.ao.Query)}
     * method, properly parameterizing the {@link net.java.ao.Query} object.</p>
     *
     * @param type The type of the entities to retrieve.
     * @param criteria A parameterized WHERE statement used to determine the results.
     * @param parameters A varargs array of parameters to be passed to the executed
     * prepared statement.  The length of this array <i>must</i> match the number of
     * parameters (denoted by the '?' char) in the <code>criteria</code>.
     * @return An array of entities of the given type which match the specified criteria.
     */
    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String criteria, Object... parameters);

    /**
     * <p>Selects all entities matching the given type and {@link net.java.ao.Query}.  By default, the
     * entities will be created based on the values within the primary key field for the
     * specified type (this is usually the desired behavior).</p>
     * <p/>
     * <p>Example:</p>
     * <p/>
     * <pre>manager.find(Person.class, Query.select().where("name LIKE ? OR age &gt; ?", "Joe", 9).limit(10));</pre>
     * <p/>
     * <p>This method delegates the call to {@link #find(Class, String, net.java.ao.Query)}, passing the
     * primary key field for the given type as the <code>String</code> parameter.</p>
     *
     * @param type The type of the entities to retrieve.
     * @param query The {@link net.java.ao.Query} instance to be used to determine the results.
     * @return An array of entities of the given type which match the specified query.
     */
    public <T extends RawEntity<K>, K> T[] find(Class<T> type, Query query);

    /**
     * <p>Selects all entities of the specified type which match the given
     * <code>Query</code>.  This method creates a <code>PreparedStatement</code>
     * using the <code>Query</code> instance specified against the table
     * represented by the given type.  This query is then executed (with the
     * parameters specified in the query).  The method then iterates through
     * the result set and extracts the specified field, mapping an entity
     * of the given type to each row.  This array of entities is returned.</p>
     *
     * @param type The type of the entities to retrieve.
     * @param field The field value to use in the creation of the entities.  This is usually
     * the primary key field of the corresponding table.
     * @param query The {@link Query} instance to use in determining the results.
     * @return An array of entities of the given type which match the specified query.
     */
    public <T extends RawEntity<K>, K> T[] find(Class<T> type, String field, Query query);
    
    /**
     * <p>Executes the specified SQL and extracts the given key field, wrapping each
     * row into a instance of the specified type.  The SQL itself is executed as
     * a {@link java.sql.PreparedStatement} with the given parameters.</p>
     * <p/>
     * <p>Example:</p>
     * <p/>
     * <pre>manager.findWithSQL(Person.class, "personID", "SELECT personID FROM chairs WHERE position &lt; ? LIMIT ?", 10, 5);</pre>
     * <p/>
     * <p>The SQL is not parsed or modified in any way by ActiveObjects.  As such, it is
     * possible to execute database-specific queries using this method without realizing
     * it.  For example, the above query will not run on MS SQL Server or Oracle, due to
     * the lack of a LIMIT clause in their SQL implementation.  As such, be extremely
     * careful about what SQL is executed using this method, or else be conscious of the
     * fact that you may be locking yourself to a specific DBMS.</p>
     *
     * @param type The type of the entities to retrieve.
     * @param keyField The field value to use in the creation of the entities.  This is usually
     * the primary key field of the corresponding table.
     * @param sql The SQL statement to execute.
     * @param parameters A varargs array of parameters to be passed to the executed
     * prepared statement.  The length of this array <i>must</i> match the number of
     * parameters (denoted by the '?' char) in the <code>criteria</code>.
     * @return An array of entities of the given type which match the specified query.
     */
    @SuppressWarnings("unchecked")
    public <T extends RawEntity<K>, K> T[] findWithSQL(Class<T> type, String keyField, String sql, Object... parameters);

    /**
     * <p>Optimised read for large datasets. This method will stream all rows for the given type to the given callback.</p>
     * 
     * <p>Please see {@link #stream(Class, Query, EntityStreamCallback)} for details / limitations.
     * 
     * @param type The type of the entities to retrieve.
     * @param streamCallback The receiver of the data, will be passed one entity per returned row 
     */    
    public <T extends RawEntity<K>, K> void stream(Class<T> type, EntityStreamCallback<T, K> streamCallback);
    
    /**
     * <p>Selects all entities of the given type and feeds them to the callback, one by one. The entities are slim, uncached, read-only
     * representations of the data. They only supports getters or designated {@link Accessor} methods. Calling setters or <pre>save</pre> will 
     * result in an exception. Other method calls will be ignored. The proxies do not support lazy-loading of related entities.</p>
     * 
     * <p>This call is optimised for efficient read operations on large datasets. For best memory usage, do not buffer the entities passed to the
     * callback but process and discard them directly.</p>
     * 
     * <p>Unlike regular Entities, the read only implementations do not support flushing/refreshing. The data is a snapshot view at the time of
     * query.</p> 
     * 
     * @param type The type of the entities to retrieve.
     * @param query 
     * @param streamCallback The receiver of the data, will be passed one entity per returned row 
     */ 
    public <T extends RawEntity<K>, K> void stream(Class<T> type, Query query, EntityStreamCallback<T, K> streamCallback);
    
    /**
     * Counts all entities of the specified type.  This method is actually
     * a delegate for: <code>count(Class&lt;? extends Entity&gt;, Query)</code>
     *
     * @param type The type of the entities which should be counted.
     * @return The number of entities of the specified type.
     */
    public <K> int count(Class<? extends RawEntity<K>> type);

    /**
     * Counts all entities of the specified type matching the given criteria
     * and parameters.  This is a convenience method for:
     * <code>count(type, Query.select().where(criteria, parameters))</code>
     *
     * @param type The type of the entities which should be counted.
     * @param criteria A parameterized WHERE statement used to determine the result
     * set which will be counted.
     * @param parameters A varargs array of parameters to be passed to the executed
     * prepared statement.  The length of this array <i>must</i> match the number of
     * parameters (denoted by the '?' char) in the <code>criteria</code>.
     * @return The number of entities of the given type which match the specified criteria.
     */
    public <K> int count(Class<? extends RawEntity<K>> type, String criteria, Object... parameters);

    /**
     * Counts all entities of the specified type matching the given {@link Query}
     * instance.  The SQL runs as a <code>SELECT COUNT(*)</code> to
     * ensure maximum performance.
     *
     * @param type The type of the entities which should be counted.
     * @param query The {@link Query} instance used to determine the result set which
     * will be counted.
     * @return The number of entities of the given type which match the specified query.
     */
    public <K> int count(Class<? extends RawEntity<K>> type, Query query);

    public <T> T executeInTransaction(TransactionCallback<T> callback);
    
}
