package org.example.database.exception;

import java.sql.SQLException;

/**
 * Wraps any other unexpected SQLException.
 */
public class DatabaseException extends OrmException {

    private final String sqlQuery;
    private final SQLException cause;

    public DatabaseException(String message, String sqlQuery, SQLException cause) {
        super(message, cause);
        this.sqlQuery = sqlQuery;
        this.cause = cause;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }

    public SQLException getCause() {
        return cause;
    }
}
