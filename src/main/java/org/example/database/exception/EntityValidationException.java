package org.example.database.exception;

public class EntityValidationException extends OrmException {
    private final Class<?> entityClass;

    public EntityValidationException(String message, Class<?> entityClass) {
        super(message);
        this.entityClass = entityClass;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }
}
