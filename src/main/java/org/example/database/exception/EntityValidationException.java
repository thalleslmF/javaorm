package org.example.database.exception;

public class EntityValidationException extends OrmException {

    public EntityValidationException(String message) {
        super(message);
    }
}