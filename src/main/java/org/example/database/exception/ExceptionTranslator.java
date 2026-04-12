package org.example.database.exception;

import java.lang.reflect.Class;
import java.sql.Connection;
import java.sql.SQLException;

public class ExceptionTranslator {
    public static OrmException translate(SQLException e, Class<?> entityClass, String sql) {
        if (e.getSQLState().equals("23505")) {
            return new DuplicateKeyException(entityClass, sql, e.getMessage());
        } else if (e.getSQLState().startsWith("23")) {
            return new DatabaseException("Integrity error executing query '" + sql + "': " + e.getMessage(), e);
        } else {
            return new DatabaseException("Database error executing query '" + sql + "': " + e.getMessage(), e);
        }
    }
}
