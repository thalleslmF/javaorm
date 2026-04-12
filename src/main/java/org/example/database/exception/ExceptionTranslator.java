package org.example.database.exception;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import java.sql.SQLException;

public class ExceptionTranslator {

    public static OrmException translate(SQLException e, Class<?> entityClass, String sql) {
        String sqlState = e.getSQLState();
        if ("23505".equals(sqlState)) {
            String constraintName = null;
            if (e instanceof PSQLException) {
                ServerErrorMessage serverErrorMessage = ((PSQLException) e).getServerErrorMessage();
                if (serverErrorMessage != null) {
                    constraintName = serverErrorMessage.getConstraint();
                }
            }
            return new DuplicateKeyException(
                    String.format("Duplicate key violation on entity '%s': unique constraint '%s'", entityClass.getSimpleName(), constraintName),
                    entityClass,
                    sql,
                    constraintName,
                    e
            );
        }
        if (sqlState != null && sqlState.startsWith("23")) {
            return new DatabaseException(
                    String.format("Database error executing query '%s': %s", sql, e.getMessage()),
                    entityClass,
                    sql,
                    e
            );
        }
        return new DatabaseException(
                String.format("Database error executing query '%s': %s", sql, e.getMessage()),
                entityClass,
                sql,
                e
        );
    }
}
