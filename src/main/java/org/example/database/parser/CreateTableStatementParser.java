package org.example.database.parser;

import org.example.database.reflection.ReflectionUtils;

import java.util.Map;
import java.util.stream.Collectors;

public class CreateTableStatementParser implements StatementParser {
    private String[] keys = {"CREATE", "TABLE"};



    @Override
    public String parse(Object object) {
        Map<String,Object> keyTypeFields  = ReflectionUtils.getKeyTypeMap(object);
        return createStatement(keyTypeFields, object.getClass().getSimpleName());
    }

    private String createStatement(Map<String, Object> keyValueFields, String name) {
        StringBuilder statement = new StringBuilder();

        for(String key : keys ) {
            statement.append(key);
            statement.append(ParserConstants.SPACE_DELIMITER);
            if (key.equals("TABLE")) {
                statement.append(name);
                statement.append(ParserConstants.SPACE_DELIMITER);
                statement.append(getTableKeyAndTypeStatement(keyValueFields));
            }
        }
        return statement.toString();
    }

    private String getTableKeyAndTypeStatement(Map<String, Object> keyValueFields) {
        return keyValueFields.entrySet().stream().map(it -> String.format("%s%s%s",it.getKey(), ParserConstants.SPACE_DELIMITER, it.getValue())).collect(Collectors.joining(",\n","(\n","\n)"));
    }


}
