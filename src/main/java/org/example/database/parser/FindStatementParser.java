package org.example.database.parser;

import org.example.database.reflection.EntityMetadata;

import java.util.stream.Collectors;

public class FindStatementParser implements StatementParser {
    private final String[] keys = {"SELECT", "FROM"};

    @Override
    public String parse(EntityMetadata metadata) {
        return createStatement(metadata);
    }

    private String createStatement(EntityMetadata metadata) {
        StringBuilder statement = new StringBuilder();
        for (String key : keys) {
            statement.append(key);
            statement.append(ParserConstants.SPACE_DELIMITER);
            if (key.equals("SELECT")) {
                statement.append(getColumns(metadata));
                statement.append(ParserConstants.SPACE_DELIMITER);
            }
            if (key.equals("FROM")) {
                statement.append(metadata.tableName);
            }
        }
        return statement.toString();
    }

    private String getColumns(EntityMetadata metadata) {
        return metadata.columns.stream().map(it -> it.columnName).collect(Collectors.joining(", "));
    }
}
