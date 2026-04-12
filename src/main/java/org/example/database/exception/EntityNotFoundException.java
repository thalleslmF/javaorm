package org.example.database.exception;

public class EntityNotFoundException extends OrmException {
    private final Class<?> entityClass;
    private final String sqlQuery;

    public EntityNotFoundException(String message, Class<?> entityClass, String sqlQuery) {
        super(message);
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
