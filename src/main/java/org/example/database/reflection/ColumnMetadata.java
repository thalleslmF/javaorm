package org.example.database.reflection;

import java.lang.reflect.Field;

public class ColumnMetadata {
    public String columnName;
    public String sqlType;
    public boolean nullable;
    public boolean unique;
    public boolean isPrimaryKey;
    public Object value;
    public Class<?> javaType;
    public Field field;

    public ColumnMetadata copy() {
        ColumnMetadata metadata = new ColumnMetadata();
        metadata.columnName = this.columnName;
        metadata.sqlType = this.sqlType;
        metadata.nullable = this.nullable;
        metadata.unique = this.unique;
        metadata.isPrimaryKey = this.isPrimaryKey;
        metadata.value = this.value;
        metadata.javaType = this.javaType;
        metadata.field = this.field;
        return metadata;
    }
}
