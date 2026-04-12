package org.example.database.exception;

/**
 * Base exception class for all ORM-related errors.
 * All ORM exceptions extend this class and are unchecked (extend RuntimeException).
 */
public class OrmException extends RuntimeException {

    public OrmException(String message) {
        super(message);
    }

    public OrmException(String message, Throwable cause) {
        super(message, cause);
    }
}
