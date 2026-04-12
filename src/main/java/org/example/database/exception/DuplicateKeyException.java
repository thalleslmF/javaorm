package org.example.database.exception;

/**
 * Thrown when INSERT violates a UNIQUE or PRIMARY KEY constraint (PostgreSQL SQLState 23505).
 */
public class DuplicateKeyException extends OrmException {

    private final Class<?> entityClass;
    private final String sqlQuery;
    private final String constraintName;

    public DuplicateKeyException(String message, Class<?> entityClass, String sqlQuery, String constraintName) {
        super(message);
        this.entityClass = entityClass;
        this.sqlQuery = sqlQuery;
        this.constraintName = constraintName;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }

    public String getConstraintName() {
        return constraintName;
    }
}
