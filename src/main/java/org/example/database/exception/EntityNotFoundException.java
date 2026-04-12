package org.example.database.exception;

import java.lang.reflect.Class;

public class EntityNotFoundException extends OrmException {
    public EntityNotFoundException(Class<?> entityClass, String sqlQuery) {
        super("No results found for entity '" + entityClass.getSimpleName() + "' with query: " + sqlQuery);
    }
}
