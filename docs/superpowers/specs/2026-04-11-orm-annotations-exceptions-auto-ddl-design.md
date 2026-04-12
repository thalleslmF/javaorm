# ORM: Annotations, Exception Hierarchy, and Auto-DDL

**Date:** 2026-04-11  
**Project:** javaorm — a learning-oriented Java ORM built on JDBC and reflection  
**Status:** Approved

---

## Background

The current ORM derives table and column names entirely from Java class and field names via reflection. There are no custom annotations, exception types are either raw `RuntimeException` or propagated `SQLException`, and schema creation (`CreateTable`) is manual.

This spec covers three interconnected features:
1. JPA-style annotations (`@Entity`, `@Table`, `@Id`, `@Column`, `@Transient`)
2. A typed exception hierarchy with contextual information
3. Auto-DDL triggered lazily per entity, configurable via `orm.properties`

---

## Approach

**Metadata layer (Approach A):** A new `EntityMetadata` class extracts annotation data from an entity class once and is cached per session. The existing parsers are updated to consume `EntityMetadata` instead of calling `ReflectionUtils` directly. Exception translation and auto-DDL logic live in `DB`. This mirrors how Hibernate separates `ClassMetadata` extraction from SQL generation.

---

## 1. Annotations

**Package:** `org.example.database.annotation`

### `@Entity`
- Target: `ElementType.TYPE`
- Retention: `RetentionPolicy.RUNTIME`
- Marks a class as an ORM-managed entity. Required on every class passed to `DB` operations.

### `@Table`
- Target: `ElementType.TYPE`
- Retention: `RetentionPolicy.RUNTIME`
- Attribute: `String name()` — defaults to `""` (ORM falls back to class simple name)
- Overrides the SQL table name.

### `@Id`
- Target: `ElementType.FIELD`
- Retention: `RetentionPolicy.RUNTIME`
- Marks the primary key field. Exactly one field per entity must carry this annotation.

### `@Column`
- Target: `ElementType.FIELD`
- Retention: `RetentionPolicy.RUNTIME`
- Attributes:
  - `String name()` — defaults to `""` (ORM falls back to field name)
  - `boolean nullable()` — defaults to `true`
  - `int length()` — defaults to `255` (applies to `varchar`)
  - `boolean unique()` — defaults to `false`

### `@Transient`
- Target: `ElementType.FIELD`
- Retention: `RetentionPolicy.RUNTIME`
- Excludes the field from all SQL operations (INSERT, SELECT, CREATE TABLE).

### Example entity

```java
@Entity
@Table(name = "users")
public class UserTest {
    @Id
    private String id;

    @Column(name = "user_name", nullable = false, length = 100)
    private String name;

    @Column
    private int age;

    @Transient
    private String internalNote;
}
```

---

## 2. Entity Validation

**Class:** `EntityValidator` in `org.example.database.reflection`

Validation runs at the beginning of every `DB` operation before any SQL is built.

Rules (in order):
1. The class must have `@Entity`. Violation → `EntityValidationException("Class X is not annotated with @Entity", entityClass)`
2. Exactly one field must have `@Id`. Zero or more than one → `EntityValidationException("Entity X must have exactly one @Id field", entityClass)`
3. The `@Id` field must not also have `@Transient`. Violation → `EntityValidationException("@Id field cannot be @Transient in entity X", entityClass)`

---

## 3. Exception Hierarchy

**Package:** `org.example.database.exception`

All exceptions are unchecked (extend `RuntimeException`) so callers are not forced to handle them, matching the pattern used by Spring's `DataAccessException`.

```
RuntimeException
└── OrmException
    ├── EntityValidationException
    ├── EntityNotFoundException
    ├── DuplicateKeyException
    └── DatabaseException
```

### `OrmException`
Base class. Constructor: `OrmException(String message)` and `OrmException(String message, Throwable cause)`.

### `EntityValidationException`
Thrown when entity structure is invalid (missing `@Entity`, `@Id` issues).  
Extra context: `Class<?> entityClass`  
Message example: `"Entity validation failed for 'UserTest': No @Id field found"`

### `EntityNotFoundException`
Thrown when `Find()` returns zero rows.  
Extra context: `Class<?> entityClass`, `String sqlQuery`  
Message example: `"No results found for entity 'UserTest' with query: SELECT id, name FROM users"`

### `DuplicateKeyException`
Thrown when INSERT violates a UNIQUE or PRIMARY KEY constraint (PostgreSQL SQLState `23505`).  
Extra context: `Class<?> entityClass`, `String sqlQuery`, `String constraintName`  
Message example: `"Duplicate key violation on entity 'UserTest': unique constraint 'users_pkey'"`

### `DatabaseException`
Wraps any other unexpected `SQLException`.  
Extra context: `String sqlQuery`, `SQLException cause`  
Message example: `"Database error executing query 'INSERT INTO users...': <original message>"`

### Exception Translation

A new `ExceptionTranslator` class (in `org.example.database.exception`) handles translation:

```java
class ExceptionTranslator {
    static OrmException translate(SQLException e, Class<?> entityClass, String sql);
}
```

Translation rules:
- SQLState `23505` → `DuplicateKeyException`
- SQLState starts with `23` (any integrity violation) → `DatabaseException`
- All other `SQLException` → `DatabaseException`

`DB.Insert`, `DB.Find`, and `DB.CreateTable` no longer declare `throws SQLException`. They catch `SQLException` internally and call `ExceptionTranslator.translate()`.

---

## 4. EntityMetadata

**Package:** `org.example.database.reflection`

### `ColumnMetadata`

```java
class ColumnMetadata {
    String columnName;    // resolved name (from @Column(name) or field name)
    String sqlType;       // SQL type string (e.g. "varchar(100)", "int", "float8")
    boolean nullable;
    boolean unique;
    boolean isPrimaryKey; // true if @Id present
    Object value;         // current field value (populated at operation time)
}
```

### `EntityMetadata`

```java
class EntityMetadata {
    String tableName;               // from @Table(name) or class simple name
    List<ColumnMetadata> columns;   // non-@Transient fields, in declaration order
    ColumnMetadata idColumn;        // the @Id field
}
```

**Factory method:** `EntityMetadata.from(Object entity)` — builds metadata from the entity instance, reading field values.

`ReflectionUtils` is updated internally to populate `EntityMetadata` instead of returning raw `Map<String, Object>`. The existing public API of `ReflectionUtils` may be removed or made package-private, as `EntityMetadata.from()` becomes the public entry point.

### Caching

`DB` holds a `Map<Class<?>, EntityMetadata>` cache. Metadata is built once per entity class per `DB` session. On subsequent calls, the cached `EntityMetadata` is reused with updated field values.

---

## 5. Auto-DDL

### Configuration

**File:** `src/main/resources/orm.properties`

```properties
# Options: create-if-not-exists, none (default: none)
orm.ddl-auto=create-if-not-exists
```

### `OrmConfig`

**Class:** `OrmConfig` in `org.example.database`

```java
class OrmConfig {
    DdlAuto ddlAuto;
    static OrmConfig load();  // reads classpath:orm.properties; returns NONE if file absent
}

enum DdlAuto {
    CREATE_IF_NOT_EXISTS,
    NONE
}
```

`DB.Open()` calls `OrmConfig.load()` and stores the result on the `DB` instance.

### Lazy trigger

`DB` holds a `Set<Class<?>> initializedEntities`.

Before any `Insert` or `Find` operation:
1. `EntityValidator.validate(entity)` is called.
2. If `ddlAuto == CREATE_IF_NOT_EXISTS` and `entityClass` is not in `initializedEntities`:
   - Execute `CREATE TABLE IF NOT EXISTS` using `CreateTableStatementParser` with `EntityMetadata`
   - Add `entityClass` to `initializedEntities`
3. If `ddlAuto == NONE` — skip auto-DDL entirely.

`db.CreateTable(entity)` remains public. Users can call it manually at any time regardless of the `ddl-auto` setting.

---

## 6. Updated Parser Contract

Parsers are updated to accept `EntityMetadata` instead of a raw `Object`:

```java
interface StatementParser {
    String parse(EntityMetadata metadata);
}
```

Each parser reads `metadata.tableName`, `metadata.columns`, and `metadata.idColumn` to build SQL. SQL type handling (currently in `ReflectionUtils.getSqlType`) moves to `ColumnMetadata` construction inside `EntityMetadata.from()`.

**Fix: type-aware value quoting** — `CreateStatementParser` currently wraps all values in single quotes. With `ColumnMetadata` carrying the Java field type, numeric types (`int`, `float`, `double`) are no longer quoted. Only `String` and other non-numeric types get single-quote wrapping.

---

## 7. Package Structure

```
org.example.database/
├── DB.java                              (updated)
├── Driver.java                          (unchanged)
├── OrmConfig.java                       (new)
├── annotation/
│   ├── Entity.java                      (new)
│   ├── Table.java                       (new)
│   ├── Id.java                          (new)
│   ├── Column.java                      (new)
│   └── Transient.java                   (new)
├── exception/
│   ├── OrmException.java                (new)
│   ├── EntityValidationException.java   (new)
│   ├── EntityNotFoundException.java     (new)
│   ├── DuplicateKeyException.java       (new)
│   ├── DatabaseException.java           (new)
│   └── ExceptionTranslator.java         (new)
├── reflection/
│   ├── ReflectionUtils.java             (updated: internal use only)
│   ├── EntityMetadata.java              (new)
│   ├── ColumnMetadata.java              (new)
│   └── EntityValidator.java             (new)
└── parser/
    ├── Parser.java                      (minor update: passes EntityMetadata)
    ├── StatementParser.java             (updated: parse(EntityMetadata))
    ├── ParserConstants.java             (unchanged)
    ├── CreateStatementParser.java       (updated: consumes EntityMetadata)
    ├── CreateTableStatementParser.java  (updated: consumes EntityMetadata)
    └── FindStatementParser.java         (updated: consumes EntityMetadata)
```

---

## 8. Testing

Existing tests (`CreateStatementTest`, `CreateTableStatementTest`, `FindStatementTest`) need updating because `StatementParser.parse()` signature changes from `Object` to `EntityMetadata`. `UserTest` in test sources needs `@Entity` and `@Id` annotations added.

New unit tests to add:
- `EntityValidatorTest` — covers all three validation failure cases
- `ExceptionTranslatorTest` — covers SQLState mapping
- `OrmConfigTest` — covers properties loading (present, absent, unknown value)
- `EntityMetadataTest` — covers annotation reading, `@Transient` exclusion, name overrides

The integration test (`CreateUserTest`) is updated to remove the manual `db.CreateTable()` call (auto-DDL will handle it) and the entity class is annotated.

---

## Out of Scope

- `UPDATE` and `VALIDATE` DDL modes
- `DELETE` operation
- WHERE clause support in `Find`
- Multi-database support (only PostgreSQL)
- Connection pooling
- Transaction management
