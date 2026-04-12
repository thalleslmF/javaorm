package org.example.database.exception;

import java.sql.SQLException;

public class ExceptionTranslator {

    private static final String UNIQUE_VIOLATION_CODE = "23505";

    public static OrmException translate(SQLException e, Class<?> entityClass, String sql) {
        String sqlState = e.getSQLState();

        if (UNIQUE_VIOLATION_CODE.equals(sqlState)) {
            String constraintName = extractConstraintName(e);
            String message = String.format("Duplicate key violation on entity '%s': unique constraint '%s'",
                    entityClass != null ? entityClass.getSimpleName() : "unknown",
                    constraintName);
            return new DuplicateKeyException(message, entityClass, sql, constraintName);
        }

        if (sqlState != null && sqlState.startsWith("23")) {
            String message = String.format("Database error executing query '%s': %s", sql, e.getMessage());
            return new DatabaseException(message, sql, e);
        }

        String message = String.format("Database error executing query '%s': %s", sql, e.getMessage());
        return new DatabaseException(message, sql, e);
    }

    private static String extractConstraintName(SQLException e) {
        String message = e.getMessage();
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
