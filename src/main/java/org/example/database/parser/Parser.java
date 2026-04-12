package org.example.database.parser;

import org.example.database.reflection.EntityMetadata;

public class Parser {
    public static String parseStatement(StatementParser statement, EntityMetadata metadata) {
        return statement.parse(metadata);
    }
}
