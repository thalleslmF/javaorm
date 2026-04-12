package org.example.database.exception;

public class DatabaseException extends OrmException {
    private final Class<?> entityClass;
    private final String sqlQuery;

    public DatabaseException(String message, Class<?> entityClass, String sqlQuery, Throwable cause) {
        super(message, cause);
        this.entityClass = entityClass;
        this.sqlQuery = sqlQuery;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }
}
