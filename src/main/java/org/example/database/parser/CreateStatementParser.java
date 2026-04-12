package org.example.database.parser;

import org.example.database.reflection.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateStatementParser implements StatementParser {
    private String[] keys = {"INSERT", "INTO", "VALUES"};

    @Override
    public String parse(Object object) {
        Map<String,Object> keyValueFields  = ReflectionUtils.getKeyValuesMap(object);
        return createStatement(keyValueFields, object.getClass().getSimpleName());
    }

    private String createStatement(Map<String, Object> keyValueFields, String name) {
        StringBuilder statement = new StringBuilder();

        for(String key : keys ) {
            statement.append(key);
            statement.append(ParserConstants.SPACE_DELIMITER);
            if (key.equals("INTO")) {
                statement.append(name);
                statement.append(getKeysStatement(keyValueFields));
                statement.append(ParserConstants.SPACE_DELIMITER);
            }
            if (key.equals("VALUES")) {
                statement.append(getValuesStatement(keyValueFields));
            }
        }
        return statement.toString();
    }

    private String getValuesStatement(Map<String, Object> keyValueFields) {
        return keyValueFields.entrySet().stream().map(it-> String.format("'%s'",it.getValue())).collect(Collectors.joining(", ","(", ")"));
    }

    private String getKeysStatement(Map<String,Object> keys) {
        return keys.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.joining(", ","(", ")"));
    }


}
