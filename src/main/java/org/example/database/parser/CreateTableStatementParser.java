package org.example.database.parser;

import org.example.database.reflection.ColumnMetadata;
import org.example.database.reflection.EntityMetadata;

import java.util.stream.Collectors;

public class CreateTableStatementParser implements StatementParser {
    private final String[] keys = {"CREATE", "TABLE"};

    @Override
    public String parse(EntityMetadata metadata) {
        return createStatement(metadata);
    }

    private String createStatement(EntityMetadata metadata) {
        StringBuilder statement = new StringBuilder();

        for (String key : keys) {
            statement.append(key);
            statement.append(ParserConstants.SPACE_DELIMITER);
            if (key.equals("TABLE")) {
                statement.append("IF NOT EXISTS");
                statement.append(ParserConstants.SPACE_DELIMITER);
                statement.append(metadata.tableName);
                statement.append(ParserConstants.SPACE_DELIMITER);
                statement.append(getTableKeyAndTypeStatement(metadata));
            }
        }
        return statement.toString();
    }

    private String getTableKeyAndTypeStatement(EntityMetadata metadata) {
        return metadata.columns.stream()
                .map(this::formatColumn)
                .collect(Collectors.joining(",\n", "(\n", "\n)"));
    }

    private String formatColumn(ColumnMetadata columnMetadata) {
        StringBuilder builder = new StringBuilder();
        builder.append(columnMetadata.columnName)
                .append(ParserConstants.SPACE_DELIMITER)
                .append(columnMetadata.sqlType);

        if (columnMetadata.isPrimaryKey) {
            builder.append(ParserConstants.SPACE_DELIMITER).append("PRIMARY KEY");
        }
        if (!columnMetadata.nullable) {
            builder.append(ParserConstants.SPACE_DELIMITER).append("NOT NULL");
        }
        if (columnMetadata.unique) {
            builder.append(ParserConstants.SPACE_DELIMITER).append("UNIQUE");
        }
        return builder.toString();
    }
}
