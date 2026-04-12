package org.example;

import org.example.database.annotation.Entity;
import org.example.database.annotation.Id;
import org.example.database.annotation.Transient;
import org.example.database.exception.EntityValidationException;
import org.example.database.reflection.EntityValidator;
import org.junit.Test;

public class EntityValidatorTest {

    @Test(expected = EntityValidationException.class)
    public void shouldFailWhenEntityAnnotationIsMissing() {
        EntityValidator.validate(NoEntityAnnotation.class);
    }

    @Test(expected = EntityValidationException.class)
    public void shouldFailWhenIdIsMissing() {
        EntityValidator.validate(NoIdEntity.class);
    }

    @Test(expected = EntityValidationException.class)
    public void shouldFailWhenMultipleIdsExist() {
        EntityValidator.validate(MultipleIdsEntity.class);
    }

    @Test(expected = EntityValidationException.class)
    public void shouldFailWhenIdIsTransient() {
        EntityValidator.validate(TransientIdEntity.class);
    }

    private static class NoEntityAnnotation {
        @Id
        private String id;
    }

    @Entity
    private static class NoIdEntity {
        private String name;
    }

    @Entity
    private static class MultipleIdsEntity {
        @Id
        private String id;
        @Id
        private String otherId;
    }

    @Entity
    private static class TransientIdEntity {
        @Id
        @Transient
        private String id;
    }
}
