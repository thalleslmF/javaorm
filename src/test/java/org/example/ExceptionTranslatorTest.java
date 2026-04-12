package org.example;

import org.example.database.exception.DatabaseException;
import org.example.database.exception.DuplicateKeyException;
import org.example.database.exception.ExceptionTranslator;
import org.example.database.exception.OrmException;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;

public class ExceptionTranslatorTest {

    @Test
    public void shouldTranslateDuplicateKey() {
        OrmException exception = ExceptionTranslator.translate(new SQLException("duplicate", "23505"), UserTest.class, "INSERT SQL");
        Assert.assertTrue(exception instanceof DuplicateKeyException);
    }

    @Test
    public void shouldTranslateIntegrityViolation() {
        OrmException exception = ExceptionTranslator.translate(new SQLException("integrity", "23514"), UserTest.class, "INSERT SQL");
        Assert.assertTrue(exception instanceof DatabaseException);
    }

    @Test
    public void shouldTranslateGenericDatabaseError() {
        OrmException exception = ExceptionTranslator.translate(new SQLException("generic", "42000"), UserTest.class, "INSERT SQL");
        Assert.assertTrue(exception instanceof DatabaseException);
    }
}
