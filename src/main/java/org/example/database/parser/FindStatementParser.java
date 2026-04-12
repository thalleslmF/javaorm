package org.example.database.parser;

import org.example.database.reflection.ReflectionUtils;

import java.util.Map;
import java.util.stream.Collectors;

public class FindStatementParser implements StatementParser {
    String[] keys = { "SELECT", "FROM"};
    @Override
    public String parse(Object object) {
        var mapKeyValue = ReflectionUtils.getKeyValuesMap(object);
        return createStatement(mapKeyValue, object.getClass().getSimpleName());
    }

    private String createStatement(Map<String, Object> mapKeyValue, String tableName) {
        var statement = new StringBuilder();
        for(var key: keys) {
            statement.append(key);
            statement.append(ParserConstants.SPACE_DELIMITER);
            if (key.equals("SELECT")) {
                statement.append(getColumns(mapKeyValue));
                statement.append(ParserConstants.SPACE_DELIMITER);
            }
            if (key.equals("FROM")) {
                statement.append(tableName);
            }
        }
        return statement.toString();
    }

    private String getColumns(Map<String, Object> mapKeyValue) {
        return mapKeyValue.entrySet().stream().map(it -> it.getKey()).collect(Collectors.joining(", ","",""));
    }
}
