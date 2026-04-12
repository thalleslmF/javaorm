package org.example.database.exception;

import java.lang.reflect.Class;

public class DuplicateKeyException extends OrmException {
    public DuplicateKeyException(Class<?> entityClass, String sqlQuery, String constraintName) {
        super("Duplicate key violation on entity '" + entityClass.getSimpleName() + "': unique constraint '" + constraintName + "'");
    }
}
