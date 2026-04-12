package org.example.database.reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class EntityMetadata {
    public String tableName;
    public List<ColumnMetadata> columns;
    public ColumnMetadata idColumn;

    public static EntityMetadata fromClass(Class<?> entityClass) {
        EntityMetadata metadata = new EntityMetadata();
        // Logic to derive table name and column metadata from annotations
        return metadata;
    }

    public EntityMetadata withValues(Object entity) {
        EntityMetadata copy = new EntityMetadata();
        copy.tableName = this.tableName;
        copy.columns = new ArrayList<>();
        for (ColumnMetadata column : this.columns) {
            ColumnMetadata newColumn = new ColumnMetadata();
            // Populate values
            copy.columns.add(newColumn);
        }
        return copy;
    }
}
