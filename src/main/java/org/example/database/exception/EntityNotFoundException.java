package org.example.database.exception;

public class EntityNotFoundException extends OrmException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}