package org.example;

import org.example.database.annotation.Column;
import org.example.database.annotation.Entity;
import org.example.database.annotation.Id;
import org.example.database.annotation.Table;
import org.example.database.annotation.Transient;
import org.example.database.reflection.EntityMetadata;
import org.junit.Assert;
import org.junit.Test;

public class EntityMetadataTest {

    @Test
    public void shouldReadAnnotationsAndValues() {
        MetadataEntity entity = new MetadataEntity();
        entity.id = "7";
        entity.name = "neo";
        entity.age = 42;
        entity.internalNote = "ignore";

        EntityMetadata structuralMetadata = EntityMetadata.fromClass(MetadataEntity.class);
        EntityMetadata valueMetadata = structuralMetadata.withValues(entity);

        Assert.assertEquals("users", structuralMetadata.tableName);
        Assert.assertEquals(3, structuralMetadata.columns.size());
        Assert.assertEquals("id", structuralMetadata.idColumn.columnName);
        Assert.assertEquals("user_name", structuralMetadata.columns.get(1).columnName);
        Assert.assertEquals("varchar(100)", structuralMetadata.columns.get(1).sqlType);
        Assert.assertEquals("7", valueMetadata.idColumn.value);
        Assert.assertEquals("neo", valueMetadata.columns.get(1).value);
        Assert.assertEquals(42, valueMetadata.columns.get(2).value);
    }

    @Entity
    @Table(name = "users")
    private static class MetadataEntity {
        @Id
        String id;

        @Column(name = "user_name", length = 100, nullable = false)
        String name;

        int age;

        @Transient
        String internalNote;
    }
}
