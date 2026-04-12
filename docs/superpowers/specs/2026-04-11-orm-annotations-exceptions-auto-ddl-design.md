# ORM: Annotations, Exception Hierarchy, and Auto-DDL

**Date:** 2026-04-11  
**Project:** javaorm — a learning-oriented Java ORM built on JDBC and reflection  
**Status:** Approved (reviewed)

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

Validation runs at the beginning of every `DB` operation, **before** `EntityMetadata.fromClass()` is called. `EntityMetadata.fromClass()` assumes a pre-validated entity class and does not re-run these checks.

Method signature: `public static void validate(Class<?> entityClass)` — accepts the entity class (not the instance). `DB` calls it as `EntityValidator.validate(entity.getClass())`.

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

**`Find` result-set fix:** The current `DB.Find` calls `resultSet.next()` which advances the cursor and consumes the first row, so returning the `ResultSet` to the caller leaves them on row 1 — skipping it. The fix uses a single consistent strategy: call `resultSet.next()` once after executing the query. If it returns `false`, throw `EntityNotFoundException`. If it returns `true`, return the `ResultSet` to the caller — the cursor is already on the first row. Callers must **not** call `next()` again before reading the first row. The `isBeforeFirst()` approach is not used.

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
public class ExceptionTranslator {
    public static OrmException translate(SQLException e, Class<?> entityClass, String sql);
}
```

Translation rules (checked in order; first match wins):
1. SQLState equals `23505` → `DuplicateKeyException`
2. SQLState starts with `23` → `DatabaseException` (other integrity violations)
3. Any other `SQLException` → `DatabaseException`

`DB.Insert`, `DB.Find`, and `DB.CreateTable` no longer declare `throws SQLException`. They catch `SQLException` internally and call `ExceptionTranslator.translate()`. For `DB.CreateTable`, the entity class is available from the entity argument (call `entity.getClass()`) and is passed to the translator as `entityClass`.

---

## 4. EntityMetadata

**Package:** `org.example.database.reflection`

### `ColumnMetadata`

```java
class ColumnMetadata {
    public String columnName;    // resolved name (from @Column(name) or field name)
    public String sqlType;       // SQL type string (e.g. "varchar(100)", "int", "float8")
    public boolean nullable;     // false forces NOT NULL; @Id always forces nullable=false
    public boolean unique;
    public boolean isPrimaryKey; // true if @Id present
    public Object value;         // current field value (only populated in withValues copies)
}
```

### `EntityMetadata`

```java
class EntityMetadata {
    public String tableName;               // from @Table(name) or class simple name
    public List<ColumnMetadata> columns;   // non-@Transient fields, in declaration order
    public ColumnMetadata idColumn;        // the @Id field
}
```

**Factory methods:**
- `public static EntityMetadata fromClass(Class<?> entityClass)` — builds **structural** metadata (table name, column names, SQL types, constraints) from the class definition. This is cached per class.
- `public EntityMetadata withValues(Object entity)` — instance method. Takes the cached structural `EntityMetadata` and produces a new instance with field values populated from the entity instance. This is called on every operation, not cached.

`ColumnMetadata` holds `value` only in the per-call copy returned by `withValues`. The cached structural metadata never holds values.

`ReflectionUtils` is updated internally to populate `EntityMetadata` instead of returning raw `Map<String, Object>`. The existing public API of `ReflectionUtils` may be removed or made package-private, as `EntityMetadata` factory methods become the public entry point.

### Caching

`DB` holds a `Map<Class<?>, EntityMetadata> schemaCache` where each entry is structural-only metadata (no values). On each operation, `DB` calls `cachedMetadata.withValues(entity)` to get a fresh per-call copy with current field values. The cache is populated on first access per entity class.

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

Before any `Insert` operation:
1. `EntityValidator.validate(entity.getClass())` is called.
2. `DB` retrieves or populates the structural `EntityMetadata` from `schemaCache` via `EntityMetadata.fromClass(entityClass)`.
3. If `ddlAuto == CREATE_IF_NOT_EXISTS` and `entityClass` is not in `initializedEntities`:
   - Execute `CREATE TABLE IF NOT EXISTS` using `CreateTableStatementParser` with the **structural** `EntityMetadata` (from `schemaCache`, no field values needed for DDL)
   - Add `entityClass` to `initializedEntities`
4. If `ddlAuto == NONE` — skip auto-DDL entirely.
5. Call `structuralMetadata.withValues(entity)` to get a per-call copy with field values.
6. Pass the value-bearing `EntityMetadata` to `CreateStatementParser`.

Before any `Find` operation:
1. `EntityValidator.validate(entity.getClass())` is called.
2. `DB` retrieves or populates the structural `EntityMetadata` from `schemaCache`.
3. Auto-DDL does **not** fire for `Find`. Automatically creating an empty table before a SELECT would result in an immediate `EntityNotFoundException` on every fresh session, which is misleading. Schema creation should only be triggered by write operations (`Insert`).
4. Call `structuralMetadata.withValues(entity)` to get a per-call copy with field values.
5. Pass the value-bearing `EntityMetadata` to `FindStatementParser`.

`db.CreateTable(entity)` remains public. Users can call it manually at any time regardless of the `ddl-auto` setting.

---

## 6. Updated Parser Contract

Parsers are updated to accept `EntityMetadata` instead of a raw `Object`:

```java
interface StatementParser {
    String parse(EntityMetadata metadata);
}
```

`Parser.java` is updated with a new signature: `public static String parseStatement(StatementParser statement, EntityMetadata metadata)`. It simply calls `statement.parse(metadata)` and returns the result.

Each parser reads `metadata.tableName`, `metadata.columns`, and `metadata.idColumn` to build SQL. SQL type handling (currently in `ReflectionUtils.getSqlType`) moves to `ColumnMetadata` construction inside `EntityMetadata.fromClass()`.

**Call chain for `Insert` and `Find`:**  
See §5 Lazy trigger for the full step-by-step. In summary: `DB` validates → retrieves/caches structural `EntityMetadata` → calls `withValues(entity)` for a per-call copy → passes that copy to the appropriate parser via `Parser.parseStatement(parser, metadata)`. For auto-DDL in `Insert`, the structural (no-values) `EntityMetadata` is passed to `CreateTableStatementParser`.

**`CreateTableStatementParser` — DDL constraint assembly:**  
`CreateTableStatementParser` now emits `CREATE TABLE IF NOT EXISTS` (updated from the current `CREATE TABLE`). `ColumnMetadata` retains `nullable`, `unique`, and `isPrimaryKey` as booleans. During `EntityMetadata.fromClass()` construction, an `@Id` field always has `nullable` forced to `false` regardless of any `@Column(nullable=...)` setting. `CreateTableStatementParser` assembles DDL constraint fragments when iterating columns:
- `nullable = false` → append `NOT NULL`
- `unique = true` → append `UNIQUE`
- `isPrimaryKey = true` → append `PRIMARY KEY`
- Constraints are appended in this order: type, then `PRIMARY KEY`, then `NOT NULL`, then `UNIQUE`
- Example output for `@Id String id`: `id varchar(255) PRIMARY KEY NOT NULL`
- Note: `PRIMARY KEY` implies `NOT NULL` in PostgreSQL — appending both is intentional for explicitness in this learning project.

The `length` attribute from `@Column` is folded into `sqlType` during `ColumnMetadata` construction: a `String` field with `length=100` produces `sqlType = "varchar(100)"`.

**SQL type mapping** (used inside `EntityMetadata.fromClass()` when building `ColumnMetadata.sqlType`):

| Java type | SQL type |
|---|---|
| `String` | `varchar(N)` where N = `@Column(length)` default 255 |
| `int` / `Integer` | `int` |
| `float` / `Float` | `real` |
| `double` / `Double` | `float8` |
| `long` / `Long` | `bigint` |
| `boolean` / `Boolean` | `boolean` |
| anything else | raw Java simple type name (logged as a warning) |

**Fix: type-aware value quoting** — `CreateStatementParser` currently wraps all values in single quotes. With `ColumnMetadata` carrying the Java field type, numeric types (`int`, `float`, `double`) are no longer quoted in INSERT values. Only `String` and other non-numeric types get single-quote wrapping. The existing `CreateStatementTest` assertion for `age` will change from `'20'` to `20` once `age` is typed as `int`.

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

Existing tests (`CreateStatementTest`, `CreateTableStatementTest`, `FindStatementTest`) need updating because `StatementParser.parse()` signature changes from `Object` to `EntityMetadata`. `UserTest` in test sources needs `@Entity` and `@Id` annotations added, and `age` changes from `String` to `int`. Assertion changes:
- `CreateStatementTest`: `age` value changes from `'20'` to `20` (no quotes for numeric type)
- `CreateTableStatementTest`: `age` column type changes from `age varchar(255)` to `age int`; the `id` column gains `PRIMARY KEY NOT NULL` constraints
- `FindStatementTest`: no assertion changes (column list is unaffected by type)

New unit tests to add:
- `EntityValidatorTest` — covers all three validation failure cases
- `ExceptionTranslatorTest` — covers SQLState mapping (in order: `23505`, `23xxx`, other)
- `OrmConfigTest` — covers: file present with valid value, file absent (defaults to `NONE`), unknown value (throws `OrmException` with a clear message — invalid `orm.ddl-auto` value is not silently ignored)
- `EntityMetadataTest` — covers annotation reading, `@Transient` exclusion, name overrides, structural cache vs. value copy

The integration test (`CreateUserTest`) is updated:
- `UserTest` is annotated with `@Entity` and `@Id`; `age` changes to `int`
- A test-scoped `orm.properties` (in `src/test/resources/`) sets `orm.ddl-auto=create-if-not-exists`
- The manual `db.CreateTable()` call is removed — auto-DDL handles it via `Insert`
- The `db.Find()` result assertions (`resultSet.getString(1)` etc.) remain unchanged: the cursor is already on the first row, which is consistent with the existing test code — no assertion change needed there. The `age` read-back changes from `resultSet.getString(3)` to `resultSet.getInt(3)` (or remain as `getString` since PostgreSQL will coerce).

---

## Out of Scope

- `UPDATE` and `VALIDATE` DDL modes
- `DELETE` operation
- WHERE clause support in `Find`
- Multi-database support (only PostgreSQL)
- Connection pooling
- Transaction management
