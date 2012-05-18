package com.atlassian.activeobjects.internal.db;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import net.java.ao.DBParam;
import net.java.ao.DatabaseProvider;
import net.java.ao.DisposableDataSource;
import net.java.ao.EntityManager;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import net.java.ao.schema.IndexNameConverter;
import net.java.ao.schema.NameConverters;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.schema.UniqueNameConverter;
import net.java.ao.schema.ddl.DDLField;
import net.java.ao.schema.ddl.DDLForeignKey;
import net.java.ao.schema.ddl.DDLIndex;
import net.java.ao.schema.ddl.DDLTable;
import net.java.ao.schema.ddl.SQLAction;
import net.java.ao.types.TypeManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;

public class SQLServerCSDatabaseProvider extends DatabaseProvider
{
    private static final Pattern VALUE_PATTERN = Pattern.compile("^\\((.*?)\\)$");

    public SQLServerCSDatabaseProvider(DisposableDataSource dataSource)
    {
        this(dataSource, "dbo");
    }

    public SQLServerCSDatabaseProvider(DisposableDataSource dataSource, String schema)
    {
        super(dataSource, schema, TypeManager.sqlServer());
    }

	@Override
	public void setQueryResultSetProperties(ResultSet res, Query query) throws SQLException
    {
		if (query.getOffset() >= 0) {
			res.absolute(query.getOffset());
		}
	}

	@Override
	public ResultSet getTables(Connection conn) throws SQLException {
		return conn.getMetaData().getTables(null, getSchema(), null, new String[] {"TABLE"});
	}

	@Override
	public Object parseValue(int type, String value) {
        if (value == null || value.equals("") || value.equals("NULL"))
        {
            return null;
        }

        Matcher valueMatcher = VALUE_PATTERN.matcher(value);
        while (valueMatcher.matches())
        {
            value = valueMatcher.group(1);
            valueMatcher = VALUE_PATTERN.matcher(value);
        }

        switch (type)
        {
            case Types.TIMESTAMP:
            case Types.DATE:
            case Types.TIME:
            case Types.VARCHAR:
                Matcher matcher = Pattern.compile("'(.*)'.*").matcher(value);
                if (matcher.find())
                {
                    value = matcher.group(1);
                }
                break;
        }

        return super.parseValue(type, value);
    }

    @Override
    protected Iterable<SQLAction> renderAlterTableChangeColumn(NameConverters nameConverters, DDLTable table, DDLField oldField, DDLField field)
    {
        final ImmutableList.Builder<SQLAction> sql = ImmutableList.builder();

        // Removing index before applying changes to columns, SQL Server doesn't like to touch columns with indexes!
        final Iterable<DDLIndex> indexes = findIndexesForField(table, field);
        for (DDLIndex index : indexes)
        {
            SQLAction sqlAction = renderDropIndex(nameConverters.getIndexNameConverter(), index);
            if (sqlAction != null)
            {
                sql.add(sqlAction);
            }
        }

        if (field.isPrimaryKey())
        {
            sql.add(SQLAction.of(new StringBuilder().append("ALTER TABLE ").append(withSchema(table.getName()))
                    .append(" DROP CONSTRAINT ").append(primaryKeyName(table.getName(), field.getName()))));
        }

        sql.addAll(super.renderAlterTableChangeColumn(nameConverters, table, oldField, field));

        if (field.isPrimaryKey())
        {
            sql.add(SQLAction.of(new StringBuilder().append("ALTER TABLE ").append(withSchema(table.getName()))
                    .append(" ADD CONSTRAINT ").append(primaryKeyName(table.getName(), field.getName()))
                    .append(" PRIMARY KEY (").append(field.getName()).append(")")));
        }

        if ((field.getDefaultValue() != null && !field.getDefaultValue().equals(oldField.getDefaultValue())) || (field.getDefaultValue() == null && oldField.getDefaultValue() != null))
        {
            sql.add(SQLAction.of(new StringBuilder()
                    .append("IF EXISTS (SELECT 1 FROM sys.objects WHERE NAME = ").append(renderValue(defaultConstraintName(table, field))).append(") ")
                    .append("ALTER TABLE ").append(withSchema(table.getName()))
                    .append(" DROP CONSTRAINT ").append(defaultConstraintName(table, field))));

            if (field.getDefaultValue() != null)
            {
                sql.add(SQLAction.of(new StringBuilder()
                        .append("ALTER TABLE ").append(withSchema(table.getName()))
                        .append(" ADD CONSTRAINT ").append(defaultConstraintName(table, field))
                        .append(" DEFAULT ").append(renderValue(field.getDefaultValue()))
                        .append(" FOR ").append(processID(field.getName()))));
            }
        }

        if (!oldField.isUnique() && field.isUnique())
        {
            sql.add(SQLAction.of(new StringBuilder()
                    .append("ALTER TABLE ").append(withSchema(table.getName()))
                    .append(" ADD CONSTRAINT ").append(nameConverters.getUniqueNameConverter().getName(table.getName(), field.getName()))
                    .append(" UNIQUE(").append(processID(field.getName())).append(")")));
        }

        if (oldField.isUnique() && !field.isUnique())
        {
            sql.add(SQLAction.of(new StringBuilder()
                    .append("ALTER TABLE ").append(withSchema(table.getName()))
                    .append(" DROP CONSTRAINT ").append(nameConverters.getUniqueNameConverter().getName(table.getName(), oldField.getName()))));
        }

        // re-adding indexes!
        for (DDLIndex index : indexes)
        {
            sql.add(renderCreateIndex(nameConverters.getIndexNameConverter(), index));
        }

        return sql.build();
    }

    private Iterable<DDLIndex> findIndexesForField(DDLTable table, final DDLField field)
    {
        return Iterables.filter(newArrayList(table.getIndexes()), new Predicate<DDLIndex>()
        {
            @Override
            public boolean apply(DDLIndex index)
            {
                return index.getField().equals(field.getName());
            }
        });
    }

    private String defaultConstraintName(DDLTable table, DDLField field)
    {
        return "df_" + table.getName() + '_' + field.getName();
    }

    @Override
    protected SQLAction renderCreateIndex(IndexNameConverter indexNameConverter, DDLIndex index)
    {
        StringBuilder back = new StringBuilder();

        back.append("CREATE INDEX ").append(processID(indexNameConverter.getName(shorten(index.getTable()), shorten(index.getField()))));
        back.append(" ON ").append(withSchema(index.getTable())).append('(').append(processID(index.getField())).append(')');

        return SQLAction.of(back);
    }

    @Override
    protected SQLAction renderDropIndex(IndexNameConverter indexNameConverter, DDLIndex index)
    {
        if (hasIndex(indexNameConverter, index))
        {
            return SQLAction.of(new StringBuilder().append("DROP INDEX ")
                    .append(processID(indexNameConverter.getName(shorten(index.getTable()), shorten(index.getField()))))
                    .append(" ON ")
                    .append(withSchema(index.getTable())));
        }
        else
        {
            return null;
        }
    }

    @Override
    protected String renderUnique(UniqueNameConverter uniqueNameConverter, DDLTable table, DDLField field)
    {
        return "CONSTRAINT " + uniqueNameConverter.getName(table.getName(), field.getName()) + " UNIQUE";
    }

    @Override
    protected RenderFieldOptions renderFieldOptionsInAlterColumn()
    {
        return new RenderFieldOptions(false, false, true);
    }

    @Override
	protected String renderQuerySelect(Query query, TableNameConverter converter, boolean count) {
		StringBuilder sql = new StringBuilder();
		String tableName = query.getTable();

		if (tableName == null) {
			tableName = converter.getName(query.getTableType());
		}

		switch (query.getType()) {
			case SELECT:
				sql.append("SELECT ");

				if (query.isDistinct()) {
					sql.append("DISTINCT ");
				}

				int limit = query.getLimit();
				if (limit >= 0) {
					if (query.getOffset() > 0) {
						limit += query.getOffset();
					}

					sql.append("TOP ").append(limit).append(' ');
				}

				if (count) {
					sql.append("COUNT(*)");
				} else {
                    sql.append(querySelectFields(query));
				}
                sql.append(" FROM ").append(queryTableName(query, converter));
			break;
		}

		return sql.toString();
	}

	@Override
	protected String renderQueryLimit(Query query) {
		return "";
	}

    protected String renderPrimaryKey(String tableName, String pkFieldName)
    {
        StringBuilder b = new StringBuilder();
        b.append("CONSTRAINT ");
        b.append(primaryKeyName(tableName, pkFieldName));
        b.append(" PRIMARY KEY(");
        b.append(processID(pkFieldName));
        b.append(")\n");
        return b.toString();
    }

    private String primaryKeyName(String tableName, String pkFieldName)
    {
        return "pk_" + tableName + "_" + pkFieldName;
    }

    @Override
	protected String renderAutoIncrement() {
		return "IDENTITY(1,1)";
	}

    @Override
    protected String renderFieldDefault(DDLTable table, DDLField field)
    {
        return new StringBuilder().append(" CONSTRAINT ").append(defaultConstraintName(table, field)).append(" DEFAULT ").append(renderValue(field.getDefaultValue())).toString();
    }

    @Override
    protected SQLAction renderAlterTableChangeColumnStatement(NameConverters nameConverters, DDLTable table, DDLField oldField, DDLField field, RenderFieldOptions options)
    {
        final boolean autoIncrement = field.isAutoIncrement();
        try
        {
            field.setAutoIncrement(false); // we don't want the autoincrement aspect of the field to be taken in account for "alter column"

            final StringBuilder current = new StringBuilder();
            current.append("ALTER TABLE ").append(withSchema(table.getName())).append(" ALTER COLUMN ");
            current.append(renderField(nameConverters, table, field, options));

            return SQLAction.of(current);
        }
        finally
        {
            field.setAutoIncrement(autoIncrement);
        }
    }

    @Override
	protected SQLAction renderAlterTableAddColumnStatement(NameConverters nameConverters, DDLTable table, DDLField field)
    {
        return SQLAction.of("ALTER TABLE " + withSchema(table.getName()) + " ADD " + renderField(nameConverters, table, field, new RenderFieldOptions(true, true, true)));
	}

	@Override
	protected SQLAction renderAlterTableDropKey(DDLForeignKey key)
	{
		StringBuilder back = new StringBuilder("ALTER TABLE ");

		back.append(withSchema(key.getDomesticTable())).append(" DROP CONSTRAINT ").append(processID(key.getFKName()));

		return SQLAction.of(back);
	}

	@Override
	@SuppressWarnings("unused")
    public synchronized <T extends RawEntity<K>, K> K insertReturningKey(EntityManager manager, Connection conn,
            Class<T> entityType, Class<K> pkType,
            String pkField, boolean pkIdentity, String table, DBParam... params) throws SQLException
    {
		boolean identityInsert = false;
		StringBuilder sql = new StringBuilder();

		if (pkIdentity) {
			for (DBParam param : params) {
				if (param.getField().trim().equalsIgnoreCase(pkField)) {
					identityInsert = true;
					sql.append("SET IDENTITY_INSERT ").append(withSchema(table)).append(" ON\n");
					break;
				}
			}
		}

		sql.append("INSERT INTO ").append(withSchema(table));

		if (params.length > 0) {
			sql.append(" (");
			for (DBParam param : params) {
				sql.append(processID(param.getField()));
				sql.append(',');
			}
			sql.setLength(sql.length() - 1);

			sql.append(") VALUES (");

			for (DBParam param : params) {
				sql.append("?,");
			}
			sql.setLength(sql.length() - 1);

			sql.append(")");
		} else {
			sql.append(" DEFAULT VALUES");
		}

		if (identityInsert) {
			sql.append("\nSET IDENTITY_INSERT ").append(processID(table)).append(" OFF");
		}

		K back = executeInsertReturningKey(manager, conn, entityType, pkType, pkField, sql.toString(), params);

		return back;
	}

	@Override
	protected Set<String> getReservedWords() {
		return RESERVED_WORDS;
	}

    private static final Set<String> RESERVED_WORDS = ImmutableSet.of(
            "ADD", "EXCEPT", "PERCENT", "ALL", "EXEC", "PLAN", "ALTER", "EXECUTE",
            "PRECISION", "AND", "EXISTS", "PRIMARY", "ANY", "EXIT", "PRINT", "AS", "FETCH", "PROC",
            "ASC", "FILE", "PROCEDURE", "AUTHORIZATION", "FILLFACTOR", "PUBLIC", "BACKUP", "FOR",
            "RAISERROR", "BEGIN", "FOREIGN", "READ", "BETWEEN", "FREETEXT", "READTEXT", "BREAK",
            "FREETEXTTABLE", "RECONFIGURE", "BROWSE", "FROM", "REFERENCES", "BULK", "FULL",
            "REPLICATION", "BY", "FUNCTION", "RESTORE", "CASCADE", "GOTO", "RESTRICT", "CASE",
            "GRANT", "RETURN", "CHECK", "GROUP", "REVOKE", "CHECKPOINT", "HAVING", "RIGHT", "CLOSE",
            "HOLDLOCK", "ROLLBACK", "CLUSTERED", "IDENTITY", "ROWCOUNT", "COALESCE", "IDENTITY_INSERT",
            "ROWGUIDCOL", "COLLATE", "IDENTITYCOL", "RULE", "COLUMN", "IF", "SAVE", "COMMIT", "IN",
            "SCHEMA", "COMPUTE", "INDEX", "SELECT", "CONSTRAINT", "INNER", "SESSION_USER", "CONTAINS",
            "INSERT", "SET", "CONTAINSTABLE", "INTERSECT", "SETUSER", "CONTINUE", "INTO", "SHUTDOWN",
            "CONVERT", "IS", "SOME", "CREATE", "JOIN", "STATISTICS", "CROSS", "KEY", "SYSTEM_USER",
            "CURRENT", "KILL", "TABLE", "CURRENT_DATE", "LEFT", "TEXTSIZE", "CURRENT_TIME", "LIKE",
            "THEN", "CURRENT_TIMESTAMP", "LINENO", "TO", "CURRENT_USER", "LOAD", "TOP", "CURSOR",
            "NATIONAL", "TRAN", "DATABASE", "NOCHECK", "TRANSACTION", "DBCC", "NONCLUSTERED",
            "TRIGGER", "DEALLOCATE", "NOT", "TRUNCATE", "DECLARE", "NULL", "TSEQUAL", "DEFAULT",
            "NULLIF", "UNION", "DELETE", "OF", "UNIQUE", "DENY", "OFF", "UPDATE", "DESC", "OFFSETS",
            "UPDATETEXT", "DISK", "ON", "USE", "DISTINCT", "OPEN", "USER", "DISTRIBUTED",
            "OPENDATASOURCE", "VALUES", "DOUBLE", "OPENQUERY", "VARYING", "DROP", "OPENROWSET",
            "VIEW", "DUMMY", "OPENXML", "WAITFOR", "DUMP", "OPTION", "WHEN", "ELSE", "OR", "WHERE",
            "END", "ORDER", "WHILE", "ERRLVL", "OUTER", "WITH", "ESCAPE", "OVER", "WRITETEXT",
            "ABSOLUTE", "EXEC", "OVERLAPS", "ACTION", "EXECUTE", "PAD", "ADA", "EXISTS", "PARTIAL",
            "ADD", "EXTERNAL", "PASCAL", "ALL", "EXTRACT", "POSITION", "ALLOCATE", "FALSE",
            "PRECISION", "ALTER", "FETCH", "PREPARE", "AND", "FIRST", "PRESERVE", "ANY", "FLOAT",
            "PRIMARY", "ARE", "FOR", "PRIOR", "AS", "FOREIGN", "PRIVILEGES", "ASC", "FORTRAN",
            "PROCEDURE", "ASSERTION", "FOUND", "PUBLIC", "AT", "FROM", "READ", "AUTHORIZATION",
            "FULL", "REAL", "AVG", "GET", "REFERENCES", "BEGIN", "GLOBAL", "RELATIVE", "BETWEEN",
            "GO", "RESTRICT", "BIT", "GOTO", "REVOKE", "BIT_LENGTH", "GRANT", "RIGHT", "BOTH",
            "GROUP", "ROLLBACK", "BY", "HAVING", "ROWS", "CASCADE", "HOUR", "SCHEMA", "CASCADED",
            "IDENTITY", "SCROLL", "CASE", "IMMEDIATE", "SECOND", "CAST", "IN", "SECTION", "CATALOG",
            "INCLUDE", "SELECT", "CHAR", "INDEX", "SESSION", "CHAR_LENGTH", "INDICATOR",
            "SESSION_USER", "CHARACTER", "INITIALLY", "SET", "CHARACTER_LENGTH", "INNER", "SIZE",
            "CHECK", "INPUT", "SMALLINT", "CLOSE", "INSENSITIVE", "SOME", "COALESCE", "INSERT",
            "SPACE", "COLLATE", "INT", "SQL", "COLLATION", "INTEGER", "SQLCA", "COLUMN", "INTERSECT",
            "SQLCODE", "COMMIT", "INTERVAL", "SQLERROR", "CONNECT", "INTO", "SQLSTATE", "CONNECTION",
            "IS", "SQLWARNING", "CONSTRAINT", "ISOLATION", "SUBSTRING", "CONSTRAINTS", "JOIN", "SUM",
            "CONTINUE", "KEY", "SYSTEM_USER", "CONVERT", "LANGUAGE", "TABLE", "CORRESPONDING", "LAST",
            "TEMPORARY", "COUNT", "LEADING", "THEN", "CREATE", "LEFT", "TIME", "CROSS", "LEVEL",
            "TIMESTAMP", "CURRENT", "LIKE", "TIMEZONE_HOUR", "CURRENT_DATE", "LOCAL", "TIMEZONE_MINUTE",
            "CURRENT_TIME", "LOWER", "TO", "CURRENT_TIMESTAMP", "MATCH", "TRAILING", "CURRENT_USER",
            "MAX", "TRANSACTION", "CURSOR", "MIN", "TRANSLATE", "DATE", "MINUTE", "TRANSLATION", "DAY",
            "MODULE", "TRIM", "DEALLOCATE", "MONTH", "TRUE", "DEC", "NAMES", "UNION", "DECIMAL",
            "NATIONAL", "UNIQUE", "DECLARE", "NATURAL", "UNKNOWN", "DEFAULT", "NCHAR", "UPDATE",
            "DEFERRABLE", "NEXT", "UPPER", "DEFERRED", "NO", "USAGE", "DELETE", "NONE", "USER", "DESC",
            "NOT", "USING", "DESCRIBE", "NULL", "VALUE", "DESCRIPTOR", "NULLIF", "VALUES", "DIAGNOSTICS",
            "NUMERIC", "VARCHAR", "DISCONNECT", "OCTET_LENGTH", "VARYING", "DISTINCT", "OF", "VIEW",
            "DOMAIN", "ON", "WHEN", "DOUBLE", "ONLY", "WHENEVER", "DROP", "OPEN", "WHERE", "ELSE",
            "OPTION", "WITH", "END", "OR", "WORK", "END-EXEC", "ORDER", "WRITE", "ESCAPE", "OUTER",
            "YEAR", "EXCEPT", "OUTPUT", "ZONE", "EXCEPTION", "ABSOLUTE", "FOUND", "PRESERVE", "ACTION",
            "FREE", "PRIOR", "ADMIN", "GENERAL", "PRIVILEGES", "AFTER", "GET", "READS", "AGGREGATE",
            "GLOBAL", "REAL", "ALIAS", "GO", "RECURSIVE", "ALLOCATE", "GROUPING", "REF", "ARE", "HOST",
            "REFERENCING", "ARRAY", "HOUR", "RELATIVE", "ASSERTION", "IGNORE", "RESULT", "AT",
            "IMMEDIATE", "RETURNS", "BEFORE", "INDICATOR", "ROLE", "BINARY", "INITIALIZE", "ROLLUP",
            "BIT", "INITIALLY", "ROUTINE", "BLOB", "INOUT", "ROW", "BOOLEAN", "INPUT", "ROWS", "BOTH",
            "INT", "SAVEPOINT", "BREADTH", "INTEGER", "SCROLL", "CALL", "INTERVAL", "SCOPE", "CASCADED",
            "ISOLATION", "SEARCH", "CAST", "ITERATE", "SECOND", "CATALOG", "LANGUAGE", "SECTION", "CHAR",
            "LARGE", "SEQUENCE", "CHARACTER", "LAST", "SESSION", "CLASS", "LATERAL", "SETS", "CLOB",
            "LEADING", "SIZE", "COLLATION", "LESS", "SMALLINT", "COMPLETION", "LEVEL", "SPACE", "CONNECT",
            "LIMIT", "SPECIFIC", "CONNECTION", "LOCAL", "SPECIFICTYPE", "CONSTRAINTS", "LOCALTIME", "SQL",
            "CONSTRUCTOR", "LOCALTIMESTAMP", "SQLEXCEPTION", "CORRESPONDING", "LOCATOR", "SQLSTATE",
            "CUBE", "MAP", "SQLWARNING", "CURRENT_PATH", "MATCH", "START", "CURRENT_ROLE", "MINUTE",
            "STATE", "CYCLE", "MODIFIES", "STATEMENT", "DATA", "MODIFY", "STATIC", "DATE", "MODULE",
            "STRUCTURE", "DAY", "MONTH", "TEMPORARY", "DEC", "NAMES", "TERMINATE", "DECIMAL", "NATURAL",
            "THAN", "DEFERRABLE", "NCHAR", "TIME", "DEFERRED", "NCLOB", "TIMESTAMP", "DEPTH", "NEW",
            "TIMEZONE_HOUR", "DEREF", "NEXT", "TIMEZONE_MINUTE", "DESCRIBE", "NO", "TRAILING", "DESCRIPTOR",
            "NONE", "TRANSLATION", "DESTROY", "NUMERIC", "TREAT", "DESTRUCTOR", "OBJECT", "TRUE",
            "DETERMINISTIC", "OLD", "UNDER", "DICTIONARY", "ONLY", "UNKNOWN", "DIAGNOSTICS", "OPERATION",
            "UNNEST", "DISCONNECT", "ORDINALITY", "USAGE", "DOMAIN", "OUT", "USING", "DYNAMIC", "OUTPUT",
            "VALUE", "EACH", "PAD", "VARCHAR", "END-EXEC", "PARAMETER", "VARIABLE", "EQUALS", "PARAMETERS",
            "WHENEVER", "EVERY", "PARTIAL", "WITHOUT", "EXCEPTION", "PATH", "WORK", "EXTERNAL", "POSTFIX",
            "WRITE", "FALSE", "PREFIX", "YEAR", "FIRST", "PREORDER", "ZONE", "FLOAT", "PREPARE");
}
