package org.example.database.reflection;

import org.example.database.annotation.Entity;
import org.example.database.annotation.Id;
import org.example.database.annotation.Transient;
import org.example.database.exception.EntityValidationException;

import java.lang.reflect.Field;

public class EntityValidator {

    public static void validate(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new EntityValidationException(
                    String.format("Class %s is not annotated with @Entity", entityClass.getSimpleName()),
                    entityClass
            );
        }

        int idCount = 0;
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                idCount++;
                if (field.isAnnotationPresent(Transient.class)) {
                    throw new EntityValidationException(
                            String.format("@Id field cannot be @Transient in entity %s", entityClass.getSimpleName()),
                            entityClass
                    );
                }
            }
        }

        if (idCount != 1) {
            throw new EntityValidationException(
                    String.format("Entity %s must have exactly one @Id field", entityClass.getSimpleName()),
                    entityClass
            );
        }
    }
}
