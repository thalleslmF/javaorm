package org.example.database.reflection;

public class ReflectionUtils {

    private ReflectionUtils() {
    }

    public static EntityMetadata getEntityMetadata(Class<?> entityClass) {
        return EntityMetadata.fromClass(entityClass);
    }
}
