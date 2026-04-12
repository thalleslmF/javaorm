package org.example.database.parser;

import org.example.database.reflection.EntityMetadata;

public interface StatementParser {

    String parse(EntityMetadata metadata);
}
