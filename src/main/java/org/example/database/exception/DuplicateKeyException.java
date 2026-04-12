package org.example.database.exception;

public class DuplicateKeyException extends OrmException {

    public DuplicateKeyException(String message) {
        super(message);
    }
}