package org.example.database.exception;

/**
 * Thrown when entity structure is invalid (missing @Entity, @Id issues).
 */
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
