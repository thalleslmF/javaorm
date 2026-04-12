package org.example.database.reflection;

import org.example.database.annotation.Column;
import org.example.database.annotation.Id;
import org.example.database.annotation.Table;
import org.example.database.annotation.Transient;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class EntityMetadata {
    public String tableName;
    public List<ColumnMetadata> columns;
    public ColumnMetadata idColumn;

    public static EntityMetadata fromClass(Class<?> entityClass) {
        EntityMetadata metadata = new EntityMetadata();
        Table table = entityClass.getAnnotation(Table.class);
        metadata.tableName = table != null && !table.name().isEmpty() ? table.name() : entityClass.getSimpleName();
        metadata.columns = new ArrayList<>();

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }

            field.setAccessible(true);
            ColumnMetadata columnMetadata = new ColumnMetadata();
            Column column = field.getAnnotation(Column.class);
            columnMetadata.columnName = column != null && !column.name().isEmpty() ? column.name() : field.getName();
            columnMetadata.nullable = column == null || column.nullable();
            columnMetadata.unique = column != null && column.unique();
            columnMetadata.isPrimaryKey = field.isAnnotationPresent(Id.class);
            if (columnMetadata.isPrimaryKey) {
                columnMetadata.nullable = false;
            }
            columnMetadata.javaType = field.getType();
            columnMetadata.sqlType = resolveSqlType(field.getType(), column);
            columnMetadata.field = field;

            metadata.columns.add(columnMetadata);
            if (columnMetadata.isPrimaryKey) {
                metadata.idColumn = columnMetadata;
            }
        }

        return metadata;
    }

    public EntityMetadata withValues(Object entity) {
        EntityMetadata metadata = new EntityMetadata();
        metadata.tableName = this.tableName;
        metadata.columns = new ArrayList<>();

        for (ColumnMetadata structuralColumn : this.columns) {
            ColumnMetadata copy = structuralColumn.copy();
            try {
                copy.value = copy.field.get(entity);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to read field value: " + copy.field.getName(), e);
            }
            metadata.columns.add(copy);
            if (copy.isPrimaryKey) {
                metadata.idColumn = copy;
            }
        }

        return metadata;
    }

    private static String resolveSqlType(Class<?> type, Column column) {
        if (String.class.equals(type)) {
            int length = column != null ? column.length() : 255;
            return "varchar(" + length + ")";
        }
        if (Integer.class.equals(type) || int.class.equals(type)) {
            return "int";
        }
        if (Float.class.equals(type) || float.class.equals(type)) {
            return "real";
        }
        if (Double.class.equals(type) || double.class.equals(type)) {
            return "float8";
        }
        if (Long.class.equals(type) || long.class.equals(type)) {
            return "bigint";
        }
        if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return "boolean";
        }
        return type.getSimpleName();
    }
}
