package org.example.database.exception;

import java.lang.reflect.Class;

public class EntityValidationException extends OrmException {
    public EntityValidationException(String message, Class<?> entityClass) {
        super("Entity validation failed for '" + entityClass.getSimpleName() + "': " + message);
    }
}
