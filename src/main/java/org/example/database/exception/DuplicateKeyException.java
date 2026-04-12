package org.example.database.exception;

public class DuplicateKeyException extends OrmException {
    private final Class<?> entityClass;
    private final String sqlQuery;
    private final String constraintName;

    public DuplicateKeyException(String message, Class<?> entityClass, String sqlQuery, String constraintName, Throwable cause) {
        super(message, cause);
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
