package org.example.database.parser;

import org.example.database.reflection.ColumnMetadata;
import org.example.database.reflection.EntityMetadata;

import java.util.stream.Collectors;

public class CreateStatementParser implements StatementParser {
    private final String[] keys = {"INSERT", "INTO", "VALUES"};

    @Override
    public String parse(EntityMetadata metadata) {
        return createStatement(metadata);
    }

    private String createStatement(EntityMetadata metadata) {
        StringBuilder statement = new StringBuilder();

        for (String key : keys) {
            statement.append(key);
            statement.append(ParserConstants.SPACE_DELIMITER);
            if (key.equals("INTO")) {
                statement.append(metadata.tableName);
                statement.append(getKeysStatement(metadata));
                statement.append(ParserConstants.SPACE_DELIMITER);
            }
            if (key.equals("VALUES")) {
                statement.append(getValuesStatement(metadata));
            }
        }
        return statement.toString();
    }

    private String getValuesStatement(EntityMetadata metadata) {
        return metadata.columns.stream()
                .map(this::formatValue)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    private String formatValue(ColumnMetadata columnMetadata) {
        Object value = columnMetadata.value;
        if (value == null) {
            return "null";
        }
        if (isNumeric(columnMetadata.javaType) || isBoolean(columnMetadata.javaType)) {
            return String.valueOf(value);
        }
        return String.format("'%s'", String.valueOf(value).replace("'", "''"));
    }

    private boolean isNumeric(Class<?> type) {
        return Integer.class.equals(type) || int.class.equals(type)
                || Float.class.equals(type) || float.class.equals(type)
                || Double.class.equals(type) || double.class.equals(type)
                || Long.class.equals(type) || long.class.equals(type);
    }

    private boolean isBoolean(Class<?> type) {
        return Boolean.class.equals(type) || boolean.class.equals(type);
    }

    private String getKeysStatement(EntityMetadata metadata) {
        return metadata.columns.stream().map(it -> it.columnName).collect(Collectors.joining(", ", "(", ")"));
    }
}
