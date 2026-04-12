package org.example.database.exception;

import java.sql.SQLException;

public class DatabaseException extends OrmException {
    public DatabaseException(String message, SQLException cause) {
        super(message, cause);
    }
}
