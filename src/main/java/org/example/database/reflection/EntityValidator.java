package org.example.database.reflection;

import java.lang.reflect.Field;

public class EntityValidator {
    public static void validate(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new EntityValidationException("Class is not annotated with @Entity", entityClass);
        }

        int idCount = 0;
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                idCount++;
            }
        }

        if (idCount != 1) {
            throw new EntityValidationException("Entity must have exactly one @Id field", entityClass);
        }
    }
}
