package org.example.database.parser;


public class Parser {
    public static String parseStatement(StatementParser statement, Object object) {
        return statement.parse(object);
    }
}
