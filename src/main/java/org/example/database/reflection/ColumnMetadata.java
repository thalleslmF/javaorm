package org.example.database.reflection;

import java.lang.reflect.Field;

public class ColumnMetadata {
    public String columnName;
    public String sqlType;
    public boolean nullable;
    public boolean unique;
    public boolean isPrimaryKey;
    public Object value;

    public static ColumnMetadata fromField(Field field) {
        ColumnMetadata metadata = new ColumnMetadata();
        metadata.columnName = field.getName();
        metadata.nullable = true;
        metadata.unique = false;
        metadata.isPrimaryKey = false;
        return metadata;
    }
}
