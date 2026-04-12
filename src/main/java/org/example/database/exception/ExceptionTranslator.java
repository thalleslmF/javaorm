package org.example.database.exception;

import java.sql.SQLException;

/**
 * Translates SQLException into typed ORM exceptions.
 */
public class ExceptionTranslator {

    private static final String UNIQUE_VIOLATION_CODE = "23505";

    /**
     * Translates a SQLException into the appropriate ORM exception.
     *
     * @param e          the SQLException to translate
     * @param entityClass the entity class involved (may be null)
     * @param sql        the SQL query that caused the error
     * @return the translated OrmException
     */
    public static OrmException translate(SQLException e, Class<?> entityClass, String sql) {
        String sqlState = e.getSQLState();

        // SQLState equals 23505 -> DuplicateKeyException
        if (UNIQUE_VIOLATION_CODE.equals(sqlState)) {
            String constraintName = extractConstraintName(e);
            String message = String.format("Duplicate key violation on entity '%s': unique constraint '%s'",
                    entityClass != null ? entityClass.getSimpleName() : "unknown",
                    constraintName);
            return new DuplicateKeyException(message, entityClass, sql, constraintName);
        }

        // SQLState starts with 23 -> DatabaseException (other integrity violations)
        if (sqlState != null && sqlState.startsWith("23")) {
            String message = String.format("Database error executing query '%s': %s", sql, e.getMessage());
            return new DatabaseException(message, sql, e);
        }

        // Any other SQLException -> DatabaseException
        String message = String.format("Database error executing query '%s': %s", sql, e.getMessage());
        return new DatabaseException(message, sql, e);
    }

    /**
     * Extracts the constraint name from the SQLException message.
     */
    private static String extractConstraintName(SQLException e) {
        String message = e.getMessage();
        // PostgreSQL typically includes the constraint name in the message
        // Example: "duplicate key value violates unique constraint "users_pkey""
        if (message != null && message.contains("unique constraint")) {
            int start = message.lastIndexOf("unique constraint \"");
            if (start != -1) {
                start += "unique constraint \"".length();
                int end = message.indexOf("\"", start);
                if (end > start) {
                    return message.substring(start, end);
                }
            }
        }
        return "unknown";
    }
}
